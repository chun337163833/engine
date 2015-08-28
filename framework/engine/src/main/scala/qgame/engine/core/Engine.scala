package qgame.engine.core

import java.io.{ File, InputStream }
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import akka.actor._
import akka.cluster.Cluster
import akka.event.{ EventStream, LoggingAdapter }
import qgame.akka.cluster.eventbus.ClusterPubSubBackendEventBus
import qgame.akka.extension.quartz.{ JobScheduler, Quartz }
import qgame.engine.component.Component.RunningComponentInfo
import qgame.engine.component.ComponentRunnerManager
import qgame.engine.component.ComponentRunnerManager.{ QueryCurrentComponentsInfo, StartComponents, StopComponents }
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine._
import qgame.engine.eventbus.{ ClusterEventStream, DefaultClusterEventStream }
import qgame.engine.i18n.Lang
import qgame.engine.libs._
import qgame.engine.plugin.{ Plugin, PluginManager }
import qgame.engine.runtime.{ RuntimeExecutionContext, EngineRuntime }
import qgame.engine.service.ServiceRegistry.Service
import qgame.engine.service.{ ServiceBroker, ServiceRegistry }

import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
abstract class Engine {
  protected implicit val context: EngineContext = {
    val currentStack = EngineContext.stack.value
    if (currentStack.isEmpty || (currentStack.head eq null)) {
      throw new EngineInitializationException(s"could not found engine context for engine :${this.getClass.getName}")
    } else {
      currentStack.head
    }
  }

  def actorSystem: ActorSystem = context.actorSystem

  def runtime: EngineRuntime = context.runtime

  lazy val scope: EngineScope = EngineScope(this)

  def name: String = context.name

  /**
   * This name must be the executable name,do not need contains ".bat"
   */
  def executeName: String

  def args: Array[String] = context.runtime.process.args

  def arguments: Array[String] = context.runtime.process.arguments

  def id = cluster.selfAddress.toString

  def pid: String = context.runtime.process.pid.getOrElse("")

  private[engine] def aroundPreStart() = preStart()

  protected def preStart(): Unit = ()

  private[engine] def start(): Future[String]

  private[engine] def aroundPreRestart() = preRestart()

  protected def preRestart(): Unit = ()

  private[engine] def aroundPostRestart() = postRestart()

  protected def postRestart(): Unit = ()

  private[engine] def stop(): Future[String]

  private[engine] def aroundPostStop() = postStop()

  protected def postStop(): Unit = ()

  def path: File = runtime.rootPath

  def classLoader: ClassLoader = runtime.classLoader

  def rootConfig: QGameConfig = runtime.rootConfig

  def config: QGameConfig = context.config

  def getFile(relativePath: String): File = {
    new File(path, relativePath)
  }

  def getExistingFile(relativePath: String): Option[File] = {
    Option(getFile(relativePath)).filter(_.exists)
  }

  def resource(name: String): Option[URL] = {
    Option(classLoader.getResource(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

  def resourceAsStream(name: String): Option[InputStream] = {
    Option(classLoader.getResourceAsStream(Option(name).map {
      case s if s.startsWith("/") => s.drop(1)
      case s => s
    }.get))
  }

  def clusterEventBus: ClusterPubSubBackendEventBus

  def clusterEventStream: ClusterEventStream

  def eventStream: EventStream

  def plugin[API >: Null <: AnyRef](clazz: Class[_ <: Plugin[API]]): API

  def pluginManager: PluginManager

  def jobScheduler: JobScheduler

  def serviceRegistry: ServiceRegistry

  def serviceBroker: ServiceBroker

  def timeService: TimeService.type

  override def toString: String = "engine :" + name + " started on :" + context.actorSystem

  def log: LoggingAdapter

  def cluster: Cluster = Cluster(context.actorSystem)

  def lang: Lang

  def queryComponentsInfo: Future[List[RunningComponentInfo]]

  def queryServiceInfo: Future[Map[String, Set[Service]]]

  def queryEngineInfo: Future[EngineInfo]

}

object Engine {
  private val engineCacheRef: AtomicReference[Map[String, Engine]] = new AtomicReference[Map[String, Engine]](Map.empty)

  def globalExecutionContext: RuntimeExecutionContext = qgame.engine.executionContext

  @tailrec
  private[engine] def cacheEngine(name: String, engine: Engine): Engine = {
    val cache = engineCacheRef.get()
    val updated = cache.updated(name, engine)

    if (engineCacheRef.compareAndSet(cache, updated)) {
      engine
    } else {
      cacheEngine(name, engine)
    }
  }

  def of(implicit self: ActorRef): Option[Engine] = {
    self.path.elements.slice(1, 2).headOption.flatMap(engineCacheRef.get().get)
  }

  private val instance = Cell[Engine](null)

  private[engine] def updateEngineInstance(engine: Engine) = instance.lazySet(engine)

  def current = instance.value

  final case class EngineStartException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

  trait EngineInnerMessage extends NoSerializationVerificationNeeded

  class EngineInitializationException(message: String) extends RuntimeException(message)

  trait LifeTime {
    private[engine] def start(): Unit

    private[engine] def stop(): Unit
  }

  private[engine] trait LifeCycle {
    def add[T <: LifeTime](lifeTime: T): T

    def onPreStart(): Unit

    def onPostStop(): Unit
  }

  private[engine] class DefaultLifeCycle extends LifeCycle {
    private var lifeTimes: List[LifeTime] = Nil

    override def add[T <: LifeTime](lifeTime: T): T = {
      lifeTimes = lifeTime :: lifeTimes
      lifeTime
    }

    override def onPostStop(): Unit = {
      lifeTimes.foreach(_.stop())
    }

    override def onPreStart(): Unit = {
      println("on pre start of lifecycle :" + lifeTimes.size)
      lifeTimes.reverse.foreach(_.start())
    }
  }
  case class EngineInfo(
    name: String,
    address: String,
    componentsInfos: List[RunningComponentInfo],
    serviceInfos: Map[String, Set[Service]]
  )
}

/**
 * abstract engine implement ,provide some default behavior of the engine
 */
abstract class AbstractEngine extends Engine with LoggingAble {
  //TODO add engine is started atomic boolean to defence call on preStart hook
  private val lifeCycle: LifeCycle = new DefaultLifeCycle
  //note here the component should not be lazy val
  val clusterEventBus: ClusterPubSubBackendEventBus = lifeCycle.add(ClusterPubSubBackendEventBus(context.actorSystem))

  val clusterEventStream: ClusterEventStream = lifeCycle.add(DefaultClusterEventStream(context.actorSystem))
  //TODO think more about this
  lazy val eventStream: EventStream = new EventStream(context.actorSystem)

  val jobScheduler: JobScheduler = Quartz(context.actorSystem).scheduler

  val pluginManager: PluginManager = lifeCycle.add(PluginManager(this))

  val serviceRegistry: ServiceRegistry = lifeCycle.add(ServiceRegistry(context.actorSystem))

  val serviceBroker: ServiceBroker = lifeCycle.add(new ServiceBroker(this, context))

  val lang: Lang = Lang.select(config.getString("i18n.default-lang").getOrElse(
    throw new IllegalArgumentException("must setup i18n.default-lang in your engine config")
  ))

  private var componentManager: ActorRef = _

  //  override def restart(restartCompletePromise: Promise[String]): Unit = {
  //    log.debug("""
  //                 |Application is going to Restart
  //                 |Reastarting....
  //                 |
  //               """.stripMargin)
  //    val executable = if (Platform.isLinux) {
  //      Array("bash", executeName)
  //    } else {
  //      Array("cmd", executeName)
  //    }
  //    val executeArgs = executable ++ args ++ arguments
  //    log.debug(s"executeArgs:$executeArgs")
  //    engineLifeCycle.addPostStopHook { () =>
  //      log.debug("executing post stop hook")
  //      Platform.execArgs(executeArgs, path)
  //      log.debug {
  //        """
  //          |Restarted
  //          |Stopping current one
  //          |Stopping.....
  //        """.stripMargin
  //      }
  //    }
  //    stop(restartCompletePromise)
  //  }

  override private[engine] def aroundPreStart(): Unit = {
    lifeCycle.onPreStart()
    config.getConfig("components") match {
      case Some(v) =>
        log.debug("engine :[{}] (aroundPreStart) is creating component manager.", name)
        componentManager = context.actorOf(Props.create(classOf[ComponentRunnerManager], this), "crm")
      case None =>
        //there is no components
        log.warning("engine :[{}] (aroundPreStart) don't have a components config section in engine config.will not start up without any components.", name)
    }
    super.aroundPreStart()
  }

  override private[engine] def start(): Future[String] = {
    val promise = Promise[String]()
    try {
      if (componentManager ne null) {
        log.debug("engine :[{}] (start) is starting components via component manager.", name)
        componentManager ! StartComponents(promise)
      } else {
        log.warning("engine :[{}] (start) don't have a components config section,so don't have a component manager,will just start without any components.", name)
        promise.trySuccess("") //TODO
      }
    } catch {
      case NonFatal(e) =>
        log.error(e, s"engine: [{}] clazz: [{}] started failed.", this.name, this.getClass)
        promise.tryFailure(e)
    }
    promise.future
  }

  override private[engine] def stop(): Future[String] = {
    if (componentManager ne null) {
      val promise = Promise[String]()
      log.debug("engine :[{}] (stop) have component manager,so request it to stop all child components.", name)
      componentManager ! StopComponents(promise)
      promise.future
    } else {
      log.warning("engine :[{}] (stop) don't have component manager.stopping now.", name)
      Future.successful(s"engine :[{$name}] stopped successfully without component manager initialized.")
    }
  }

  override private[engine] def aroundPostStop(): Unit = {
    super.aroundPostStop()
    if (componentManager ne null) {
      log.debug("engine :[{}] (aroundPostStop) have component manager,so stop it now.", name)
      context.stop(componentManager)
    }
    lifeCycle.onPostStop()
  }

  override def plugin[API >: Null <: AnyRef](clazz: Class[_ <: Plugin[API]]): API = pluginManager.plugin(clazz)

  override def timeService: TimeService.type = lifeCycle.add { TimeService.init(this.actorSystem); TimeService }

  override def executeName: String = "engine"

  override def queryComponentsInfo: Future[List[RunningComponentInfo]] = {
    if (componentManager ne null) {
      val promise = Promise[List[RunningComponentInfo]]()
      componentManager ! QueryCurrentComponentsInfo(promise)
      promise.future
    } else {
      Future.failed(new IllegalStateException("component manager not started"))
    }
  }

  override def queryServiceInfo: Future[Map[String, Set[Service]]] = {
    serviceRegistry.queryServicesLocally
  }

  override def queryEngineInfo: Future[EngineInfo] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val cf = queryComponentsInfo
    val sf = queryServiceInfo
    for {
      componentsInfo <- cf
      servicesInfo <- sf
    } yield EngineInfo(name, cluster.selfAddress.toString, componentsInfo, servicesInfo)
  }
}

class DefaultEngine extends AbstractEngine

object TimeService extends LifeTime {
  private val currentTime = Cell(System.currentTimeMillis())
  private var cancelable: Cancellable = _
  private var actorSystem: ActorSystem = _
  private[core] def init(system: ActorSystem) = {
    this.actorSystem = system
  }

  override def start(): Unit = {
    val initDelay: FiniteDuration = FiniteDuration(0, TimeUnit.SECONDS)
    val tickDelay = FiniteDuration(1, TimeUnit.SECONDS)
    import scala.concurrent.ExecutionContext.Implicits.global
    cancelable = actorSystem.scheduler.schedule(initDelay, tickDelay) {
      currentTime.set(System.currentTimeMillis())
    }
  }

  override def stop(): Unit = {
    cancelable.cancel()
  }

  /**
   * never call this method if you are not run on engine
   */
  def timeTickInSeconds(): Long = currentTime.value
}

