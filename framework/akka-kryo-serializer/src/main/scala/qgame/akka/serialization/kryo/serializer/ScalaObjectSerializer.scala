package qgame.akka.serialization.kryo.serializer

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class ScalaObjectSerializer[T](value: T) extends Serializer[T] {
  override def write(kryo: Kryo, output: Output, t: T): Unit = {}

  override def read(kryo: Kryo, input: Input, clazz: Class[T]): T = value
}
