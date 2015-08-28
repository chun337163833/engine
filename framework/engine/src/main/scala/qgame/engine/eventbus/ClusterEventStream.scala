package qgame.engine.eventbus

import java.util.UUID

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{ Key, ORSet }
import akka.event.ActorEventBus
import qgame.engine.core.Engine.LifeTime
import qgame.engine.eventbus.DefaultClusterEventStream.SendSupportActor.WaitingReplyTimeOutException
import qgame.engine.eventbus.DefaultClusterEventStream.{ ClusterEventStreamMessage, SendSupportActor }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, Future, Promise }

/**
 * Created by kerr.
 */

abstract class ClusterEventStream extends ActorEventBus {
  override type Classifier = String

  override type Event = ClusterEventStreamMessage

  def publish(to: Classifier, message: AnyRef)

  def send(event: Event)

  def send(to: Classifier, message: AnyRef)

  def send(event: Event, as: ActorRef)

  def send(to: Classifier, message: AnyRef, as: ActorRef)

  def sendWithReply(event: Event, timeout: FiniteDuration): Future[Any]

  def sendWithReply(to: Classifier, message: AnyRef, timeout: FiniteDuration): Future[Any]

  def subscribe(subscriber: Subscriber, to: Classifier, timeout: FiniteDuration)

  def subscribeAsync(subscriber: Subscriber, to: Classifier, timeout: FiniteDuration): Future[Boolean]

  def unsubscribe(subscriber: Subscriber, from: Classifier, timeout: FiniteDuration): Boolean

  def unsubscribeAsync(subscriber: Subscriber, from: Classifier, timeout: FiniteDuration): Future[Boolean]
}

private[engine] class DefaultClusterEventStream(system: ActorSystem) extends ClusterEventStream with LifeTime {

  import ClusterEventStreamActor._

  private var clusterEventStreamActor: ActorRef = _

  override def start(): Unit = {
    clusterEventStreamActor = system.actorOf(Props.create(classOf[ClusterEventStreamActor], this), "clusterEventStream")
  }

  override def stop(): Unit = {
    clusterEventStreamActor ! PoisonPill
  }

  override def subscribe(subscriber: Subscriber, to: Classifier): Boolean = {
    Await.result(subscribeAsync(subscriber, to, defaultMaxAwait), defaultMaxAwait)
  }

  override def subscribe(subscriber: Subscriber, to: Classifier, timeout: FiniteDuration): Unit = {
    Await.result(subscribeAsync(subscriber, to, timeout), timeout)
  }

  override def subscribeAsync(subscriber: Subscriber, to: Classifier, timeout: FiniteDuration): Future[Boolean] = {
    val promise = Promise[Boolean]()
    clusterEventStreamActor ! Subscribe(subscriber, to, promise, timeout)
    promise.future
  }

  override def publish(event: Event): Unit = {
    clusterEventStreamActor ! Publish(event)
  }

  override def publish(to: Classifier, message: AnyRef): Unit = publish(ClusterEventStreamMessage(to, message))

  override def send(event: Event): Unit = {
    clusterEventStreamActor ! Send(event)
  }

  override def send(to: Classifier, message: AnyRef): Unit = send(ClusterEventStreamMessage(to, message))

  override def send(event: Event, as: ActorRef): Unit = {
    clusterEventStreamActor ! SendAs(event, as)
  }

  override def send(to: Classifier, message: AnyRef, as: ActorRef): Unit = send(ClusterEventStreamMessage(to, message), as)

  override def sendWithReply(event: Event, timeout: FiniteDuration): Future[Any] = {
    val promise = Promise[Any]()
    clusterEventStreamActor ! SendWithReplay(event, timeout, promise)
    promise.future
  }

  override def sendWithReply(to: Classifier, message: AnyRef, timeout: FiniteDuration): Future[Any] = sendWithReply(ClusterEventStreamMessage(to, message), timeout)

  override def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = {
    Await.result(unsubscribeAsync(subscriber, from, defaultMaxAwait), defaultMaxAwait)
  }

  override def unsubscribe(subscriber: Subscriber, from: Classifier, timeout: FiniteDuration): Boolean = {
    Await.result(unsubscribeAsync(subscriber, from, timeout), timeout)
  }

  override def unsubscribeAsync(subscriber: Subscriber, from: Classifier, timeout: FiniteDuration): Future[Boolean] = {
    val promise = Promise[Boolean]()
    clusterEventStreamActor ! UnSubscribe(subscriber, from, promise, timeout)
    promise.future
  }

  override def unsubscribe(subscriber: Subscriber): Unit = {
    clusterEventStreamActor ! UnSubscribe(subscriber, null, null, null)
  }

  private class ClusterEventStreamActor extends Actor with ActorLogging {

    import ClusterEventStreamActor._

    private implicit val cluster = Cluster(context.system)
    //classifier -> subscribers
    private var subscribers: Map[String, Set[ActorRef]] = Map.empty
    //subscriber -> classifiers
    private var subscribeMapping: Map[ActorRef, Set[String]] = Map.empty

    import akka.cluster.ddata._

    private val replicator = DistributedData(context.system).replicator
    private var updateCallback: Map[String, Promise[Boolean]] = Map.empty

    private def cacheUpdatePromise(promise: Promise[Boolean]) = {
      val key = UUID.randomUUID().toString
      updateCallback = updateCallback.updated(key, promise)
      key
    }

    override def receive: Receive = {
      case Subscribe(subscriber, to, promise, timeout) =>
        val key = topicKey(to)
        if (!subscribers.contains(key.topic)) {
          import akka.cluster.ddata.Replicator._
          log.debug("key : {} have not registered yet,registering", key)
          replicator ! Subscribe(key, self)
        }
        replicator ! Update(key, WriteAll(timeout), Some(cacheUpdatePromise(promise))) {
          case Some(v) => v + subscriber
          case None => ORSet.empty[ActorRef]
        }
        context.watch(subscriber)

      case UpdateSuccess(updateKey, updateHint: Some[String] @unchecked) =>
        log.debug("update success for key:{}", updateKey)
        updateHint.foreach {
          hint =>
            updateCallback.get(hint).foreach(_.trySuccess(true))
            updateCallback = updateCallback - hint
        }
      case UpdateTimeout(updateKey, updateHint: Some[String] @unchecked) =>
        log.debug("update timeout for key :{}", updateKey)
        updateHint.foreach {
          hint =>
            updateCallback.get(hint).foreach(_.trySuccess(false))
            updateCallback = updateCallback - hint
        }

      case failure @ ModifyFailure(key, errorMessage, cause, request) =>
        log.error(cause, "update failure for key:{},message :[{}]", key, errorMessage)
        request match {
          case Some(updateHint: String) =>
            updateCallback.get(updateHint).foreach(_.trySuccess(false))
            updateCallback = updateCallback - updateHint
          case Some(anyOther) =>
            log.debug("uncatched context value :{}", anyOther)
          case None =>
        }

      case c @ Changed(topicKey: TopicKey) =>
        val currentSubscribers = c.get(topicKey)
        subscribers.get(topicKey.id) match {
          case Some(subSubscribes) =>
            subscribers = subscribers.updated(topicKey.id, currentSubscribers.elements)
            //updated
            log.debug("changed for key :{}", currentSubscribers)
          case None =>
            //new added
            subscribers = subscribers.updated(topicKey.id, currentSubscribers.elements)
            log.debug("added for key :{}", topicKey)
        }
        currentSubscribers.elements.foreach {
          subscriber =>
            subscribeMapping.get(subscriber) match {
              case Some(classifiers) =>
                subscribeMapping = subscribeMapping.updated(subscriber, classifiers + topicKey.id)
              case None =>
                subscribeMapping = subscribeMapping.updated(subscriber, Set(topicKey.id))
            }
        }
      case Publish(event) =>
        subscribers.get(topicKey(event.to).id).foreach {
          subSubscribers =>
            subSubscribers.foreach(_.!(event.message)(ActorRef.noSender))
        }
      case Send(event) =>
        subscribers.get(topicKey(event.to).id).foreach {
          subSubscribers =>
            subSubscribers.headOption.foreach(_.!(event.message)(ActorRef.noSender))
        }
      case SendAs(event, as) =>
        subscribers.get(topicKey(event.to).id).foreach {
          subSubscribers =>
            subSubscribers.headOption.foreach(_.!(event.message)(as))
        }
      case SendWithReplay(event, timeout, promise) =>
        subscribers.get(topicKey(event.to).id).foreach {
          subSubscribers =>
            val sender = context.actorOf(Props.create(classOf[SendSupportActor], event.to, timeout, promise))
            if (subSubscribers.isEmpty) {
              promise.tryFailure(new IllegalStateException(s"no receiver current for key:${event.to}"))
            } else {
              subSubscribers.headOption.foreach(_.!(event.message)(sender))
            }
        }
      case UnSubscribe(subscriber, from, promise, timeout) =>
        if (from ne null) {
          replicator ! Update(
            topicKey(from),
            ORSet.empty[ActorRef],
            WriteAll(timeout),
            Some(cacheUpdatePromise(promise))
          )(_ - subscriber)
        } else {
          //then this get a little complicated
          subscribeMapping.get(subscriber) match {
            case Some(classifiers) =>
              val subFutures = classifiers.map {
                topic =>
                  val subPromise = Promise[Boolean]()
                  replicator ! Update(topicKey(topic), ORSet.empty[ActorRef], WriteAll(timeout), Some(cacheUpdatePromise(subPromise)))(_ - subscriber)
                  subPromise.future
              }
              import scala.concurrent.ExecutionContext.Implicits.global
              promise.tryCompleteWith(Future.reduce(subFutures)(_ && _))
            case None =>
              promise.trySuccess(false)
          }
        }
      case Terminated(localSubscriber) =>
        log.debug("local Subscriber terminated :{}", localSubscriber)
        subscribeMapping.get(localSubscriber).foreach {
          classifiers =>
            classifiers.foreach {
              classifier =>
                replicator ! Update(topicKey(classifier), ORSet.empty[ActorRef], WriteLocal)(_ - localSubscriber)
            }
        }
    }

    @throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      super.preStart()
      //      import akka.contrib.datareplication.Replicator._
      //      replicator ! Subscribe("", self)
    }

    @throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      super.postStop()
      subscribeMapping.keys.foreach(context.unwatch)
    }

  }

  private object ClusterEventStreamActor {

    import scala.concurrent.duration._

    val defaultMaxAwait = 5.seconds

    def topicKey(topic: String) = TopicKey(s"topic->$topic")

    final case class TopicKey(topic: String) extends Key[ORSet[ActorRef]](topic)

    sealed trait ClusterEventStreamCommand

    case class Subscribe(subscriber: Subscriber, to: Classifier, promise: Promise[Boolean], timeout: FiniteDuration) extends ClusterEventStreamCommand

    case class Publish(event: Event) extends ClusterEventStreamCommand

    case class UnSubscribe(subscriber: Subscriber, from: Classifier, promise: Promise[Boolean], timeout: FiniteDuration) extends ClusterEventStreamCommand

    case class Send(event: Event) extends ClusterEventStreamCommand

    case class SendAs(event: Event, as: ActorRef) extends ClusterEventStreamCommand

    case class SendWithReplay(event: Event, timeout: FiniteDuration, promise: Promise[Any]) extends ClusterEventStreamCommand

  }

}

private[engine] object DefaultClusterEventStream {

  def apply(actorSystem: ActorSystem) = new DefaultClusterEventStream(actorSystem)

  case class ClusterEventStreamMessage(to: String, message: AnyRef) {
    require(to ne null, "to should not be null")
    require(message ne null, "message should not be null")
  }

  private[eventbus] class SendSupportActor(key: String, timeout: FiniteDuration, promise: Promise[Any]) extends Actor {
    private var cancelable: Cancellable = _

    override def receive: Actor.Receive = {
      case msg =>
        promise.trySuccess(msg)
        cancelable.cancel()
        context.stop(self)
    }

    @throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      super.preStart()
      import scala.concurrent.ExecutionContext.Implicits.global
      cancelable = context.system.scheduler.scheduleOnce(timeout) {
        promise.tryFailure(new WaitingReplyTimeOutException(s"time out for waiting replay for send key :$key,timeout:$timeout"))
      }
    }
  }

  private[eventbus] object SendSupportActor {

    class WaitingReplyTimeOutException(message: String) extends RuntimeException(message)

  }

}
