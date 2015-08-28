package qgame.akka.serialization.kryo.serializer

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class SomeSerializer[T] extends Serializer[Some[T]] {
  override def write(kryo: Kryo, output: Output, some: Some[T]): Unit = {
    kryo.writeClassAndObject(output, some.get)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[Some[T]]): Some[T] = {
    Some(kryo.readClassAndObject(input).asInstanceOf[T])
  }
}
