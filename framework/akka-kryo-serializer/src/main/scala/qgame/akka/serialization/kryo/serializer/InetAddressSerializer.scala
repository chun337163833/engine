package qgame.akka.serialization.kryo.serializer

import java.net.InetAddress

import com.esotericsoftware.kryo.io.{ Output, Input }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class InetAddressSerializer extends Serializer[InetAddress] {
  override def write(kryo: Kryo, output: Output, addr: InetAddress): Unit = {
    output.writeString(addr.getHostAddress)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[InetAddress]): InetAddress = {
    InetAddress.getByName(input.readString())
  }
}
