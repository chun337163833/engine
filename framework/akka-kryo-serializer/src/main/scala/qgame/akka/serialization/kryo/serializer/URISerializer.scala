package qgame.akka.serialization.kryo.serializer

import java.net.URI

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class URISerializer extends Serializer[URI] {
  override def write(kryo: Kryo, output: Output, uri: URI): Unit = {
    output.writeString(uri.toString)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[URI]): URI = {
    URI.create(input.readString())
  }
}
