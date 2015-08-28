package qgame.akka.serialization.kryo.serializer

import java.net.InetSocketAddress

import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.{ Kryo, Serializer }

/**
 * Created by kerr.
 */
class InetSocketAddressSerializer extends Serializer[InetSocketAddress] {
  override def write(kryo: Kryo, output: Output, inetAddr: InetSocketAddress): Unit = {
    output.writeString(inetAddr.getHostString)
    output.writeInt(inetAddr.getPort, true)
  }

  override def read(kryo: Kryo, input: Input, clazz: Class[InetSocketAddress]): InetSocketAddress = {
    new InetSocketAddress(input.readString(), input.readInt(true))
  }
}
