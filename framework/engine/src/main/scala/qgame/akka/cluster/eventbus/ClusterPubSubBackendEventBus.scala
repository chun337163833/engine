package qgame.akka.cluster.eventbus

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Unsubscribe, Publish, Subscribe }
import akka.event.ActorEventBus
import akka.pattern.AskTimeoutException
import qgame.engine.core.Engine.LifeTime

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, Promise }

/**
 * Created by kerr.
 */
class ClusterPubSubBackendEventBus(actorSystem: ActorSystem) extends ActorEventBus with LifeTime {
  override type Classifier = String
  override type Event = ClusterEventBusMessage
  private val mediator = DistributedPubSub(actorSystem).mediator
  private val roles: Set[String] = Cluster.get(actorSystem).selfRoles
  private var registry: Map[ActorRef, Set[String]] = Map.empty.withDefault(actorRef => Set.empty[String])

  override def start(): Unit = ()

  override def stop(): Unit = ()

  override def subscribe(subscriber: Subscriber, to: Classifier): Boolean = {
    registry.synchronized {
      if (roles.isEmpty) {
        mediator.tell(Subscribe(to, subscriber), subscriber)
        registry + (subscriber -> (registry(subscriber) + to))
        true
      } else {
        val clusterTopics = roles.map { role =>
          role + to
        }
        clusterTopics.foreach(clusterTopic => mediator.tell(Subscribe(clusterTopic, subscriber), subscriber))
        registry + (subscriber -> (registry(subscriber) ++ clusterTopics))
        true
      }
    }
  }

  override def publish(event: Event): Unit = {
    if (event.role eq null) {
      mediator ! Publish(event.topic, event)
    } else {
      mediator ! Publish(event.role + event.topic, event)
    }
  }

  //for ask support
  def publish(event: Event, sender: ActorRef): Unit = {
    if (event.role eq null) {
      mediator.tell(Publish(event.topic, event), sender)
    } else {
      mediator.tell(Publish(event.role + event.topic, event), sender)
    }
  }

  override def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = {
    registry.synchronized {
      if (roles.isEmpty) {
        mediator.tell(Unsubscribe(from, subscriber), subscriber)
        registry + (subscriber -> (registry(subscriber) - from))
        true
      } else {
        val clusterTopics = roles.map(role => role + from)
        clusterTopics.foreach(clusterTopic => mediator.tell(Unsubscribe(clusterTopic, subscriber), subscriber))
        registry + (subscriber -> (registry(subscriber) -- clusterTopics))
        true
      }
    }

  }

  override def unsubscribe(subscriber: Subscriber): Unit = {
    registry.synchronized {
      registry(subscriber).foreach {
        clusterTopic => mediator.tell(Unsubscribe(clusterTopic, subscriber), subscriber)
      }
      registry -= subscriber
    }
  }

  def ask(msg: ClusterEventBusMessage, timeout: FiniteDuration): Future[AnyRef] = {
    val promise: Promise[AnyRef] = Promise[AnyRef]()
    val askSupportActor = actorSystem.actorOf(Props.create(classOf[ASKSupportActor], this, promise, timeout))
    publish(msg.copy(sender = askSupportActor))
    //    publish(msg,askSupportActor)
    promise.future
  }

  class ASKSupportActor(promise: Promise[AnyRef], timeout: FiniteDuration) extends Actor {
    private var timeoutTask: Cancellable = _
    override def receive: Receive = {
      case TimeOutTick =>
        promise.failure(new AskTimeoutException("ask time out"))
        context.stop(self)
      case msg: AnyRef =>
        timeoutTask.cancel()
        promise.success(msg)
        context.stop(self)
    }

    @throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      import context.dispatcher
      timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeOutTick)
    }

    case object TimeOutTick
  }
}

object ClusterPubSubBackendEventBus {
  var eventBusCache: Map[ActorSystem, Option[ClusterPubSubBackendEventBus]] = Map.empty.withDefault(system => None)

  def apply(actorSystem: ActorSystem) = new ClusterPubSubBackendEventBus(actorSystem)

  def get(actorSystem: ActorSystem): Option[ClusterPubSubBackendEventBus] = eventBusCache.synchronized {
    eventBusCache(actorSystem) match {
      case None =>
        val eventBus = Some(ClusterPubSubBackendEventBus(actorSystem))
        eventBusCache += actorSystem -> eventBus
        eventBus
      case someEventBus @ Some(_) => someEventBus
    }
  }
}