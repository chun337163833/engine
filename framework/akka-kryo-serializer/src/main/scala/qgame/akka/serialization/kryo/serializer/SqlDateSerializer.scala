package qgame.akka.serialization.kryo.serializer

import java.sql.Date

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class SqlDateSerializer extends Serializer[Date] {
  override def write(kryo: Kryo, output: Output, date: Date): Unit = {
    output.writeLong(date.getTime, true)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[Date]): Date = {
    new Date(input.readLong(true))
  }
}
