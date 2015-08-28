package qgame.engine.logging.kafka

import java.io.{ PrintWriter, StringWriter }

import akka.event.LoggingAdapter
import kafka.serializer.Decoder
import qgame.engine.logging.kafka.LoggingMessage.{ LoggingEvent, LoggingLevel }

import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
class LoggingEventDecoder(logger: LoggingAdapter, meta: String) extends Decoder[LoggingEvent] {
  private val source = s"LoggingEventDecoder-$meta"
  override def fromBytes(bytes: Array[Byte]): LoggingEvent = {
    try {
      LoggingEvent.parseFrom(bytes)
    } catch {
      case NonFatal(e) =>
        //TODO add more configurable information
        logger.error(e, "error when decoder the LoggingEvent message,recovering with current stack trace.")
        LoggingEvent.newBuilder()
          .setLevel(LoggingLevel.ERROR)
          .setMessage("could not parse message")
          .setSource(source)
          .setStackTrace {
            val writer = new StringWriter()
            e.printStackTrace(new PrintWriter(writer))
            writer.toString
          }.build()
    }
  }
}
