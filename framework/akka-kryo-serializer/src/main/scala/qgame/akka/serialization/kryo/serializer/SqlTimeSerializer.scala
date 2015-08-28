package qgame.akka.serialization.kryo.serializer

import java.sql.Time

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class SqlTimeSerializer extends Serializer[Time] {
  override def write(kryo: Kryo, output: Output, time: Time): Unit = {
    output.writeLong(time.getTime, true)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[Time]): Time = {
    new Time(input.readLong(true))
  }
}
