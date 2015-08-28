package qgame.akka.cluster.eventbus

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.Cluster
import akka.event.{ ActorEventBus, EventBus, LookupClassification }
import akka.pattern.AskTimeoutException
import qgame.akka.cluster.router.RoleRouters

import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

/**
 * Created by kerr.
 */
trait ClusterEventBus {
  this: EventBus =>
  def system: ActorSystem

  def send(msg: ClusterEventBusMessage): ClusterEventBusHandlerBuilder

  def ask(msg: ClusterEventBusMessage)(implicit timeout: Long = 0): Future[AnyRef]
}

trait ClusterEventBusHandlerBuilder {
  def onReceive(f: (AnyRef, ActorRef) => Unit): ClusterEventBusHandlerBuilder

  def timeOut(timeOut: Long)(f: () => Unit): ClusterEventBusHandlerBuilder

  def onException(f: Throwable => Unit): ClusterEventBusHandlerBuilder

  def run(): Unit
}

private case class ClusterEventBusHandlerBuilderImpl(msg: ClusterEventBusMessage, roleRouters: RoleRouters, actorSystem: ActorSystem) extends ClusterEventBusHandlerBuilder {

  private var onReceiveList: List[(AnyRef, ActorRef) => Unit] = Nil
  private var timeOutHandler: (Long, () => Unit) = _
  private var onExceptionHandler: (Throwable) => Unit = _

  override def onReceive(f: (AnyRef, ActorRef) => Unit): ClusterEventBusHandlerBuilder = {
    onReceiveList = f :: onReceiveList
    this
  }

  override def run(): Unit = {
    //start a inner actor to do that thing
    val taskActor = actorSystem.actorOf(Props(classOf[SendSupportActor], this, msg, roleRouters, onReceiveList, timeOutHandler, onExceptionHandler))
    taskActor ! StartTick
  }

  private case object TimeOutTick

  private case object StartTick

  override def timeOut(timeOut: Long)(f: () => Unit): ClusterEventBusHandlerBuilder = {
    timeOutHandler = (timeOut, f)
    this
  }

  override def onException(f: (Throwable) => Unit): ClusterEventBusHandlerBuilder = {
    onExceptionHandler = f
    this
  }

  class SendSupportActor(
      msg: ClusterEventBusMessage,
      roleRouters: RoleRouters,
      onReceiveList: List[(AnyRef, ActorRef) => Unit],
      timeOutHandler: (Long, () => Unit),
      onExceptionHandler: (Throwable) => Unit
  ) extends Actor {
    var timeOutTask: Cancellable = _
    override def receive: Actor.Receive = {
      case StartTick =>
        roleRouters.tell(msg.role, msg.copy(sender = self), self)
        if (timeOutHandler ne null) {
          import context.dispatcher
          context.system.scheduler.scheduleOnce(Duration(timeOutHandler._1, TimeUnit.SECONDS), self, TimeOutTick)
        }
      case TimeOutTick =>
        timeOutHandler._2()
        context.stop(self)
      case msg: Object => {
        if (timeOutTask ne null) {
          timeOutTask.cancel()
        }
        try {
          onReceiveList.foreach {
            f => f(msg, sender())
          }
        } catch {
          case e: Throwable =>
            if (onExceptionHandler eq null)
              throw e
            else
              onExceptionHandler.apply(e)
        }
        context.stop(self)
      }
    }

  }

}

abstract class ClusterActorEventBus extends ActorEventBus with ClusterEventBus

object ClusterActorEventBus {
  var eventBusCache: Map[ActorSystem, Option[ClusterActorEventBus]] = Map.empty.withDefault(system => None)

  def get(actorSystem: ActorSystem): Option[ClusterActorEventBus] = eventBusCache.synchronized {
    eventBusCache(actorSystem) match {
      case None =>
        val eventBus = Some(DefaultClusterEventBus(actorSystem))
        eventBusCache += actorSystem -> eventBus
        eventBus
      case someEventBus @ Some(_) => someEventBus
    }
  }
}

case class DefaultClusterEventBus(actorSystem: ActorSystem) extends ClusterActorEventBus with LookupClassification {
  override type Classifier = String
  override type Event = ClusterEventBusMessage
  private val roles: Set[String] = Cluster.get(actorSystem).selfRoles
  private val roleRouters = RoleRouters.get(actorSystem).withReceiver(actorSystem.actorOf(Props(classOf[EventStreamActor], this), "clusterEventStreamActor")).start()

  override def system: ActorSystem = actorSystem

  override protected def mapSize(): Int = 128

  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }

  override def publish(event: Event): Unit = {
    val role = event.role
    val topic = event.topic
    if (roles(role)) {
      //in local?
      val iterator = subscribers.valueIterator(topic)
      if (iterator.isEmpty) {
        //not in local,need publish to cluster
        publishToCluster(event)
      } else {
        super.publish(event)
      }
    } else {
      publishToCluster(event)
    }

    def publishToCluster(event: Event): Unit = {
      roleRouters.tell(event.role, event, event.sender)
    }
  }

  override protected def classify(event: Event): Classifier = event.topic

  //below is for implement the send and ask

  override def send(msg: ClusterEventBusMessage): ClusterEventBusHandlerBuilder = {
    ClusterEventBusHandlerBuilderImpl(msg, roleRouters, actorSystem)
  }

  override def ask(askMsg: ClusterEventBusMessage)(implicit timeout: Long = -1): Future[AnyRef] = {
    val promise = Promise[AnyRef]
    val builder = send(askMsg)
    if (timeout > 0) {
      builder.timeOut(timeout) {
        () => promise.failure(new AskTimeoutException("timeout", null))
      }
    }
    builder.onReceive {
      (message, _) =>
        promise.success(message)
    }.run()
    promise.future
  }

  class EventStreamActor extends Actor {

    override def receive: Receive = {
      case event: Event => publish(event)
    }
  }

}

//
case class ClusterEventBusMessage(role: String = null, topic: String, msg: AnyRef, sender: ActorRef)

object Test {
  def test(eventbus: DefaultClusterEventBus): Unit = {

    eventbus.publish(ClusterEventBusMessage("db", "playerMap", "message", ActorRef.noSender))

    eventbus.subscribe(ActorRef.noSender, "myTopic")

    eventbus.send(ClusterEventBusMessage("db", "playerMap", "message", ActorRef.noSender))
      .onReceive {
        (msg, sender) =>
          println("msg :" + msg + "sender: " + sender)
      }.timeOut(10) {
        () => println("time out")
      }.onException(e => println(e)).run()

    //    eventbus.ask(ClusterEventBusSendMessage("db","playerMap","message"))
    //            .onComplete{
    //      case Failure(e) => println("error :"+e)
    //      case Success(v) => println(v)
    //    }
  }
}