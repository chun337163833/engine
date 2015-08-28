package qgame.akka.extension.quartz

import java.util.TimeZone

import akka.actor.{ ActorRef, ActorSystem, Cancellable }
import org.quartz._
import org.quartz.core.jmx.JobDataMapSupport
import org.quartz.impl.DirectSchedulerFactory
import org.quartz.simpl.{ RAMJobStore, SimpleThreadPool }
import qgame.akka.extension.quartz.QuartzScheduler.{ QuartzSchedulerCancellable, QuartzSchedulerConfig }
import qgame.engine.config.QGameConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * Created by kerr.
 */
trait JobScheduler {
  def schedule(
    identity: JobIdentity,
    cron: Cron,
    receiver: ActorRef,
    message: AnyRef
  )(implicit executor: ExecutionContext): Cancellable

  def schedule(identity: JobIdentity, cron: Cron)(f: => Unit)(implicit executor: ExecutionContext): Cancellable

  def schedule(identity: JobIdentity, cron: Cron,
    runnable: Runnable)(implicit executor: ExecutionContext): Cancellable

  def schedule(
    identity: JobIdentity,
    delay: FiniteDuration,
    interval: FiniteDuration,
    repeatCount: Int,
    receiver: ActorRef,
    message: AnyRef
  )(implicit executor: ExecutionContext): Cancellable

  def schedule(
    identity: JobIdentity,
    delay: FiniteDuration,
    interval: FiniteDuration,
    repeatCount: Int
  )(f: ⇒ Unit)(
    implicit
    executor: ExecutionContext
  ): Cancellable

  def schedule(
    identity: JobIdentity,
    delay: FiniteDuration,
    interval: FiniteDuration,
    repeatCount: Int,
    runnable: Runnable
  )(implicit executor: ExecutionContext): Cancellable

  def scheduleOnce(
    identity: JobIdentity,
    delay: FiniteDuration,
    receiver: ActorRef,
    message: AnyRef
  )(implicit executor: ExecutionContext): Cancellable

  def scheduleOnce(identity: JobIdentity, delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable

  def scheduleOnce(
    identity: JobIdentity,
    delay: FiniteDuration,
    runnable: Runnable
  )(implicit executor: ExecutionContext): Cancellable
}

case class JobIdentity(name: String, group: String, description: String)

case class Cron(expression: String) {
  require(CronExpression.isValidExpression(expression), s"must provide a valid cron expression,you provided $expression")
}

//TODO make this split to akka extension
private[quartz] class QuartzScheduler(
    quartzSchedulerConfig: QuartzSchedulerConfig,
    actorSystem: ActorSystem
) extends JobScheduler {

  private var timeZone: TimeZone = _

  private var scheduler: Scheduler = _
  start()

  def start(): Unit = {
    timeZone = TimeZone.getTimeZone(quartzSchedulerConfig.defaultTimezone)
    scheduler = {
      val threadPool = {
        import quartzSchedulerConfig.ThreadPool._
        val pool = new SimpleThreadPool(threadCount, threadPriority)
        pool.setMakeThreadsDaemons(threadAsDaemon)
        pool.setThreadNamePrefix(threadPrefix)
        pool
      }

      val jobStore = new RAMJobStore

      val jobScheduler = {
        import quartzSchedulerConfig._
        DirectSchedulerFactory.getInstance().createScheduler(name, instanceId, threadPool, jobStore)
        DirectSchedulerFactory.getInstance().getScheduler(name)
      }
      jobScheduler
    }
    scheduler.start()
  }

  def stop(): Unit = {
    scheduler.shutdown()
  }

  protected def scheduleJob(jobDetail: JobDetail, trigger: Trigger): Cancellable = {
    scheduler.scheduleJob(jobDetail, trigger)
    new QuartzSchedulerCancellable(scheduler, jobDetail.getKey)
  }

  private def buildJobDetail(identity: JobIdentity, receiver: ActorRef, message: AnyRef)(implicit executor: ExecutionContext): JobDetail = {
    val jobDataMap = {
      import QuartzScheduler._

      import scala.collection.JavaConverters._
      Map(executorKey -> executor, receiverKey -> receiver, messageKey -> message).asJava
    }
    val jobData = JobDataMapSupport.newJobDataMap(jobDataMap)

    JobBuilder.newJob(classOf[ActorMessageReplyJob])
      .setJobData(jobData)
      .withIdentity(identity.name, identity.group)
      .withDescription(identity.description)
      .storeDurably(false)
      .build()
  }

  private def buildJobDetail(identity: JobIdentity, f: => Unit)(implicit executor: ExecutionContext): JobDetail = {
    buildJobDetail(identity, new Runnable {
      override def run(): Unit = f
    })
  }

  private def buildJobDetail(identity: JobIdentity, runnable: Runnable)(implicit executor: ExecutionContext): JobDetail = {
    val jobDataMap = {
      import QuartzScheduler._

      import scala.collection.JavaConverters._
      Map(executorKey -> executor, runnableKey -> runnable).asJava
    }
    val jobData = JobDataMapSupport.newJobDataMap(jobDataMap)
    JobBuilder.newJob(classOf[RunnableExecutionJob])
      .setJobData(jobData)
      .withIdentity(identity.name, identity.group)
      .withDescription(identity.description)
      .storeDurably(false)
      .build()
  }

  private def buildTrigger(identity: JobIdentity, cron: Cron): Trigger = {
    TriggerBuilder.newTrigger()
      .withDescription(identity.description)
      .withIdentity(identity.name, identity.group)
      .withSchedule(CronScheduleBuilder.cronSchedule(cron.expression)
        .inTimeZone(timeZone)
        .withMisfireHandlingInstructionFireAndProceed())
      .startNow()
      .build()
  }

  override def schedule(identity: JobIdentity, cron: Cron, receiver: ActorRef, message: AnyRef)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, receiver, message)
    val trigger = buildTrigger(identity, cron)
    scheduleJob(jobDetail = jobDetail, trigger = trigger)
  }

  override def schedule(identity: JobIdentity, cron: Cron)(f: => Unit)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, f)
    val trigger = buildTrigger(identity, cron)
    scheduleJob(jobDetail, trigger)
  }

  override def schedule(identity: JobIdentity, cron: Cron, runnable: Runnable)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, runnable)
    val trigger = buildTrigger(identity, cron)
    scheduleJob(jobDetail, trigger)
  }

  private def buildTrigger(identity: JobIdentity, delay: FiniteDuration, interval: FiniteDuration, repeatCount: Int): Trigger = {
    TriggerBuilder.newTrigger()
      .withDescription(identity.description)
      .withIdentity(identity.name, identity.group)
      .withSchedule(SimpleScheduleBuilder.simpleSchedule()
        .withIntervalInMilliseconds(interval.toMillis)
        .withRepeatCount(repeatCount)
        .withMisfireHandlingInstructionFireNow())
      .startAt(DateBuilder.futureDate(delay.toMillis.toInt, DateBuilder.IntervalUnit.MILLISECOND))
      .build()
  }

  override def schedule(identity: JobIdentity, delay: FiniteDuration, interval: FiniteDuration, repeatCount: Int, receiver: ActorRef, message: AnyRef)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, receiver, message)
    val trigger = buildTrigger(identity, delay, interval, repeatCount)
    scheduleJob(jobDetail, trigger)
  }

  override def schedule(identity: JobIdentity, delay: FiniteDuration, interval: FiniteDuration, repeatCount: Int)(f: => Unit)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, f)
    val trigger = buildTrigger(identity, delay, interval, repeatCount)
    scheduleJob(jobDetail, trigger)
  }

  override def schedule(identity: JobIdentity, delay: FiniteDuration, interval: FiniteDuration, repeatCount: Int, runnable: Runnable)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, runnable)
    val trigger = buildTrigger(identity, delay, interval, repeatCount)
    scheduleJob(jobDetail, trigger)
  }

  private def buildTrigger(identity: JobIdentity, delay: FiniteDuration): Trigger = {
    TriggerBuilder.newTrigger()
      .withDescription(identity.description)
      .withIdentity(identity.name, identity.group)
      .withSchedule(SimpleScheduleBuilder.simpleSchedule()
        .withMisfireHandlingInstructionFireNow())
      .startAt(DateBuilder.futureDate(delay.toMillis.toInt, DateBuilder.IntervalUnit.MILLISECOND))
      .build()
  }

  override def scheduleOnce(identity: JobIdentity, delay: FiniteDuration, receiver: ActorRef, message: AnyRef)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, receiver, message)
    val trigger = buildTrigger(identity, delay)
    scheduler.scheduleJob(jobDetail, trigger)
    new QuartzSchedulerCancellable(scheduler, jobDetail.getKey)
  }

  override def scheduleOnce(identity: JobIdentity, delay: FiniteDuration)(f: => Unit)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, f)
    val trigger = buildTrigger(identity, delay)
    scheduler.scheduleJob(jobDetail, trigger)
    new QuartzSchedulerCancellable(scheduler, jobDetail.getKey)
  }

  override def scheduleOnce(identity: JobIdentity, delay: FiniteDuration, runnable: Runnable)(implicit executor: ExecutionContext): Cancellable = {
    val jobDetail = buildJobDetail(identity, runnable)
    val trigger = buildTrigger(identity, delay)
    scheduler.scheduleJob(jobDetail, trigger)
    new QuartzSchedulerCancellable(scheduler, jobDetail.getKey)
  }
}

private[quartz] object QuartzScheduler {

  class QuartzSchedulerCancellable(private val scheduler: Scheduler, private val jobKey: JobKey) extends Cancellable {
    require(scheduler ne null, "scheduler could not be null")
    require(jobKey ne null, "jobKey should not be null")

    override def cancel(): Boolean = scheduler.deleteJob(jobKey)

    override def isCancelled: Boolean = !scheduler.checkExists(jobKey)
  }

  val executorKey = "executor"
  val receiverKey = "receiver"
  val messageKey = "message"
  val runnableKey = "runnable"

  def apply(config: QGameConfig, actorSystem: ActorSystem): QuartzScheduler = new QuartzScheduler(QuartzSchedulerConfig(config), actorSystem)

  case class QuartzSchedulerConfig(config: QGameConfig) {
    val name = config.getString("name").getOrElse(
      throw new IllegalArgumentException("must setup quartz scheduler name")
    )

    val instanceId = config.getString("instance-id").getOrElse(
      throw new IllegalArgumentException("must setup quartz scheduler instance id")
    )

    object ThreadPool {
      val threadCount = config.getInt("thread-pool.thread-count").getOrElse(
        throw new IllegalArgumentException("must setup quartz scheduler 's thread pool 's thread count")
      )

      val threadPriority = config.getInt("thread-pool.thread-priority").getOrElse(
        throw new IllegalArgumentException("must setup quartz scheduler 's thread pool 's thread priority")
      )

      val threadPrefix = config.getString("thread-pool.thread-prefix").getOrElse(
        throw new IllegalArgumentException("must setup quartz scheduler 's thread pool 's thread prefix")
      )

      val threadAsDaemon = config.getBoolean("thread-pool.daemon").getOrElse(
        throw new IllegalArgumentException("must setup quartz scheduler 's thread pool 's thread as daemon")
      )
    }

    val defaultTimezone = config.getString("default-timezone").getOrElse(
      throw new IllegalArgumentException("must setup quartz scheduler default timezone")
    )
  }

}

private[quartz] sealed trait QuartzJob extends Job {
  def as[T](key: String)(implicit jobDataMap: JobDataMap): T = {
    Option(jobDataMap.get(key)) match {
      case Some(value) =>
        value.asInstanceOf[T]
      case None =>
        throw new IllegalArgumentException(s"don't have an element for key: $key")
    }
  }

  def maybeAs[T](key: String)(implicit jobDataMap: JobDataMap): Option[T] = {
    Option(jobDataMap.get(key)).map(_.asInstanceOf[T])
  }
}

private[quartz] class ActorMessageReplyJob extends QuartzJob {
  override def execute(context: JobExecutionContext): Unit = {
    val jobDetail = context.getJobDetail
    implicit val jobMap = context.getMergedJobDataMap
    import QuartzScheduler._
    val jobTask = for {
      executor <- maybeAs[ExecutionContext](executorKey)
      receiver <- maybeAs[ActorRef](receiverKey)
      message <- maybeAs[AnyRef](messageKey)
    } yield (executor, receiver, message)

    jobTask match {
      case Some((executor, receiver, message)) =>
        //        log.debug("executing job :name:{},group:{},description:{}", jobDetail.getKey.getName, jobDetail.getKey.getGroup, jobDetail.getDescription)
        executor.prepare().execute(new Runnable {
          override def run(): Unit = {
            receiver ! message
          }
        })
      case None =>
        throw new IllegalArgumentException(s"job task corruption job:type:actor name:${jobDetail.getKey.getName}, group:${jobDetail.getKey.getGroup}, identity:${jobDetail.getDescription}")
    }
  }
}

private[quartz] class RunnableExecutionJob extends QuartzJob {
  override def execute(context: JobExecutionContext): Unit = {
    val jobDetail = context.getJobDetail
    implicit val jobMap = context.getMergedJobDataMap
    import QuartzScheduler._
    val jobTask = for {
      executor <- maybeAs[ExecutionContext](executorKey)
      runnable <- maybeAs[Runnable](runnableKey)
    } yield (executor, runnable)

    jobTask match {
      case Some((executor, runnable)) =>
        //        log.debug("executing job :name:{},group:{},description:{}", jobDetail.getKey.getName, jobDetail.getKey.getGroup, jobDetail.getDescription)
        executor.prepare().execute(runnable)
      case None =>
        throw new IllegalArgumentException(s"job task corruption job:type:runnable name:${jobDetail.getKey.getName}, group:${jobDetail.getKey.getGroup}, identity:${jobDetail.getDescription}")
    }
  }
}

object CronDSL {

  sealed trait CronExpr

  sealed trait FinalCronExpr {
    def expr: Cron
  }

  object Daily {

    case class HourMinuteSecond(hour: Int, minute: Int, second: Int) extends FinalCronExpr {
      require(hour >= 0 && hour <= 23, "hour must hour >=0 && hour <= 23")
      require(minute >= 0 && hour <= 59, "minute must minute >=0 && minute <= 59")
      require(second >= 0 && second <= 59, "second must second >=0 && second <= 59")

      override def expr: Cron = Cron(s"$second $minute $hour ? * *")
    }

    def at(hour: Int, minute: Int, second: Int): HourMinuteSecond = HourMinuteSecond(hour, minute, second)
  }

  def daily: Daily.type = Daily

  object Weekly {
    val SUNDAY: Int = 1
    val MONDAY: Int = 2
    val TUESDAY: Int = 3
    val WEDNESDAY: Int = 4
    val THURSDAY: Int = 5
    val FRIDAY: Int = 6
    val SATURDAY: Int = 7

    case class WeekDay(weekDay: Int) {
      require(weekDay >= 1 && weekDay <= 7, "week day must weekDay >= 1 && weekDay <= 7")

      case class HourMinuteSecond(hour: Int, minute: Int, second: Int) extends FinalCronExpr {
        require(hour >= 0 && hour <= 23, "hour must hour >=0 && hour <= 23")
        require(minute >= 0 && hour <= 59, "minute must minute >=0 && minute <= 59")
        require(second >= 0 && second <= 59, "second must second >=0 && second <= 59")

        override def expr: Cron = Cron(s"$second $minute $hour ? * $weekDay")
      }

      def at(hour: Int, minute: Int, second: Int): HourMinuteSecond = HourMinuteSecond(hour, minute, second)
    }

    def at(weekDay: Int) = WeekDay(weekDay)
  }

  def weekly: Weekly.type = Weekly

  object Monthly {

    case class MonthDay(monthDay: Int) {
      require(monthDay > 1 && monthDay <= 31, "monthDay must >= 1 && monthDay <= 31")

      case class HourMinuteSecond(hour: Int, minute: Int, second: Int) extends FinalCronExpr {
        require(hour >= 0 && hour <= 23, "hour must hour >=0 && hour <= 23")
        require(minute >= 0 && hour <= 59, "minute must minute >=0 && minute <= 59")
        require(second >= 0 && second <= 59, "second must second >=0 && second <= 59")

        override def expr: Cron = Cron(s"$second $minute $second $monthDay * ?")
      }

      def at(hour: Int, minute: Int, second: Int): HourMinuteSecond = HourMinuteSecond(hour, minute, second)
    }

    def at(monthDay: Int) = MonthDay(monthDay: Int)
  }

  def monthDay: Monthly.type = Monthly

}