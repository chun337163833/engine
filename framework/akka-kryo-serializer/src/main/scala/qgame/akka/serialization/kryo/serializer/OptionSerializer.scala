package qgame.akka.serialization.kryo.serializer

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class OptionSerializer[T] extends Serializer[Option[T]] {
  override def write(kryo: Kryo, output: Output, option: Option[T]): Unit = {
    kryo.writeClassAndObject(output, option.orNull)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[Option[T]]): Option[T] = {
    Option(kryo.readClassAndObject(input).asInstanceOf[T])
  }
}
