package qgame.engine.logging.kafka

import java.io.{ PrintWriter, StringWriter }

import akka.actor.{ Actor, ActorLogging }
import akka.event.Logging._
import akka.stream.actor.WatermarkRequestStrategy
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import com.softwaremill.react.kafka.{ ProducerProperties, ReactiveKafka }
import qgame.engine.config.QGameConfig
import qgame.engine.logging.kafka.KafkaLogger.KafkaLoggerConfig
import qgame.engine.logging.kafka.LoggingMessage.LoggingEvent.LoggingMDCEntity
import qgame.engine.logging.kafka.LoggingMessage.{ LoggingEvent, LoggingLevel }

import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
class KafkaLogger extends Actor with ActorLogging {
  implicit val system = context.system
  private implicit val materializer = ActorMaterializer()
  private val kafka = new ReactiveKafka()
  private val config = new KafkaLoggerConfig(QGameConfig(context.system.settings.config.getConfig("akka.logger.kafka")))
  private val subscriber = kafka.publish(props = ProducerProperties(
    brokerList = config.brokerList,
    topic = config.topic,
    clientId = config.clientId,
    encoder = new LoggingEventEncoder
  ), requestStrategy = () => WatermarkRequestStrategy(highWatermark = config.highWatermark, lowWatermark = config.lowWatermark))
  private val flow = Source.actorRef(config.bufferSize, overflowStrategy = config.overflowStrategy).to(sink = Sink(subscriber))

  private val kafkaProxyActor = flow.run()

  override def receive: Receive = {
    case event @ Error(cause, logSource, logClass, message) =>
      withThreadContext(logSource, logClass, event) { builder =>
        builder.setLevel(LoggingLevel.ERROR)
        cause match {
          case Error.NoCause | null => builder.setMessage(String.valueOf(message))
          case _ => builder.setMessage(if (message != null) message.toString else cause.getLocalizedMessage)
        }
        builder.setStackTrace {
          val writer = new StringWriter()
          cause.printStackTrace(new PrintWriter(writer))
          writer.toString
        }
        builder.build()
      } {
        kafkaProxyActor ! _
      }
    case event @ Warning(logSource, logClass, message) =>
      withThreadContext(logSource, logClass, event) { builder =>
        builder.setLevel(LoggingLevel.WARNING)
        builder.setMessage(String.valueOf(message)).build()
      } {
        kafkaProxyActor ! _
      }
    case event @ Info(logSource, logClass, message) =>
      withThreadContext(logSource, logClass, event) { builder =>
        builder.setLevel(LoggingLevel.INFO)
        builder.setMessage(String.valueOf(message)).build()
      } {
        kafkaProxyActor ! _
      }
    case event @ Debug(logSource, logClass, message) =>
      withThreadContext(logSource, logClass, event) { builder =>
        builder.setLevel(LoggingLevel.DEBUG)
        builder.setMessage(String.valueOf(message)).build()
      } {
        kafkaProxyActor ! _
      }
    case InitializeLogger(_) =>
      log.info("log4j2 InitializeLogger done")
      sender() ! LoggerInitialized
  }

  final def withThreadContext(logSource: String, logClass: Class[_], logEvent: LogEvent)(handler: LoggingEvent.Builder => LoggingEvent)(thunk: LoggingEvent => Unit): Unit = {
    val builder = LoggingEvent.newBuilder()
    builder.setHost(config.host)
    builder.setSource(logSource)
    builder.setThread(logEvent.thread.getName)
    builder.setTimestamp(logEvent.timestamp)
    builder.setLogClass(logClass.getName)
    logEvent.mdc.foreach {
      case (key, value) => builder.addMdc(LoggingMDCEntity.newBuilder().setKey(key).setValue(String.valueOf(value)).build())
    }
    try {
      thunk(handler(builder))
    } catch {
      case NonFatal(e) =>
        log.error(e, "error when dump it to kafka logger with thread context")
    }
  }
}

object KafkaLogger {

  class KafkaLoggerConfig(config: QGameConfig) {
    val host = config.getString("host").getOrElse(
      throw new IllegalArgumentException("please setup host in akka.logger.kafka section.")
    )
    val brokerList = config.getString("broker-list").getOrElse(
      throw new IllegalArgumentException("please setup broker-list in akka.logger.kafka section.")
    )
    val topic = config.getString("topic").getOrElse(
      throw new IllegalArgumentException("please setup topic akka.logger.kafka section.")
    )

    val clientId = config.getString("client-id").getOrElse(
      throw new IllegalArgumentException("please setup client-id akka.logger.kafka section.")
    )

    val highWatermark = config.getInt("high-water-mark").getOrElse(
      throw new IllegalArgumentException("please setup high-water-mark akka.logger.kafka section.")
    )

    val lowWatermark = config.getInt("low-water-mark").getOrElse(
      throw new IllegalArgumentException("please setup low-water-mark akka.logger.kafka section.")
    )

    val bufferSize = config.getInt("buffer-size").getOrElse(
      throw new IllegalArgumentException("please setup buffer-size akka.logger.kafka section.")
    )

    val overflowStrategy = config.getString("overflow-strategy").getOrElse(
      throw new IllegalArgumentException("please setup overflow-strategy akka.logger.kafka section.")
    ) match {
        case "dropHead" => OverflowStrategy.dropHead
        case "dropTail" => OverflowStrategy.dropTail
        case "dropBuffer" => OverflowStrategy.dropBuffer
        case "dropNew" => OverflowStrategy.dropNew
        case "fail" => OverflowStrategy.fail
        case _ => OverflowStrategy.dropHead
      }
  }

}
