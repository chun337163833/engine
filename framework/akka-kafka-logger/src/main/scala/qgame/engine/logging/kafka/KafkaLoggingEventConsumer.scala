package qgame.engine.logging.kafka

import akka.actor.{ Actor, ActorLogging, Props, Terminated }
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorSubscriberMessage.{ OnComplete, OnError, OnNext }
import akka.stream.actor.{ ActorSubscriber, RequestStrategy, WatermarkRequestStrategy }
import akka.stream.scaladsl.{ Sink, Source }
import com.softwaremill.react.kafka.{ ConsumerProperties, ReactiveKafka }
import qgame.engine.logging.kafka.LoggingMessage.LoggingEvent

/**
 * Created by kerr.
 */
class KafkaLoggingEventConsumer(
    brokerList: String,
    zooKeeperHost: String,
    topic: String,
    groupId: String,
    props: Props
) extends Actor with ActorLogging {
  implicit val system = context.system
  private implicit val materializer = ActorMaterializer()
  private val kafka = new ReactiveKafka()

  private val publisher = kafka.consume(props = ConsumerProperties(
    brokerList = brokerList,
    zooKeeperHost = zooKeeperHost,
    topic = topic,
    groupId = groupId,
    decoder = new LoggingEventDecoder(log, s"\nbookerList:$brokerList\nzooKeeperHost:$zooKeeperHost\ntopic:$topic\ngroupId:$groupId")
  ))

  private val flow = Source(publisher).to(Sink.actorSubscriber(props))
  flow.run()
  log.info("KafkaLoggingEventConsumer start")

  override def receive: Receive = {
    case Terminated(ref) =>

  }
}

object KafkaLoggingEventConsumer {
  def props(
    brokerList: String,
    zooKeeperHost: String,
    topic: String,
    groupId: String,
    props: Props
  ) = Props(new KafkaLoggingEventConsumer(brokerList, zooKeeperHost, topic, groupId, props))
}

/**
 * override this class to provide the pumper
 */
abstract class AbstractLoggingEventPumper extends ActorSubscriber with ActorLogging {
  override def receive: Receive = {

    case OnNext(msg) =>
      //log.debug("receive upstream message :[{}]", String.valueOf(msg))
      handleLoggingEvent(msg.asInstanceOf[LoggingEvent])
    case OnError(e) =>
      log.error(e, "error when pumping logging event,upstream error.")
      handleUpstreamError(e)
    case OnComplete =>
      log.info("upstream complete,pumping logging event stopped.")
      handleUpstreamComplete()
  }

  def handleUpstreamError(e: Throwable): Unit = {
    context.stop(self)
  }

  def handleUpstreamComplete(): Unit = {
    context.stop(self)
  }

  def handleLoggingEvent(event: LoggingEvent): Unit

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(1024, 10)
}
