package qgame.akka.serialization.kryo.serializer

import java.sql.Timestamp

import com.esotericsoftware.kryo.{ Serializer, Kryo }
import com.esotericsoftware.kryo.io.{ Input, Output }

/**
 * Created by kerr.
 */
class TimestampSerializer extends Serializer[Timestamp] {
  override def write(kryo: Kryo, output: Output, timestamp: Timestamp): Unit = {
    output.writeLong(timestamp.getTime, true)
    output.writeInt(timestamp.getNanos, true)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[Timestamp]): Timestamp = {
    val timestamp = new Timestamp(input.readLong(true))
    timestamp.setNanos(input.readInt(true))
    timestamp
  }
}