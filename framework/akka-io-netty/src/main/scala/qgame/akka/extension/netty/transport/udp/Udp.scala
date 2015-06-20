package qgame.akka.extension.netty.transport.udp

import akka.actor.{ ActorRef, ExtendedActorSystem, ExtensionKey }
import akka.io.IO.Extension

/**
 * Created by kerr.
 */
object Udp extends ExtensionKey[UdpExt] {

}

class UdpExt(system: ExtendedActorSystem) extends Extension {
  override def manager: ActorRef = ???
}
