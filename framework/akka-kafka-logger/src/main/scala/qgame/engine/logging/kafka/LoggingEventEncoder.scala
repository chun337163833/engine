package qgame.engine.logging.kafka

import kafka.serializer.Encoder
import qgame.engine.logging.kafka.LoggingMessage.LoggingEvent

/**
 * Created by kerr.
 */
class LoggingEventEncoder extends Encoder[LoggingEvent] {
  override def toBytes(t: LoggingEvent): Array[Byte] = t.toByteArray
}
