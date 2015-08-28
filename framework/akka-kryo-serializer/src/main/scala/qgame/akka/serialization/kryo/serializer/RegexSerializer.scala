package qgame.akka.serialization.kryo.serializer

import java.util.regex.Pattern

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class RegexSerializer extends Serializer[Pattern] {
  override def write(kryo: Kryo, output: Output, pattern: Pattern): Unit = {
    output.writeString(pattern.pattern())
    output.writeInt(pattern.flags(), true)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[Pattern]): Pattern = {
    Pattern.compile(input.readString(), input.readInt(true))
  }
}
