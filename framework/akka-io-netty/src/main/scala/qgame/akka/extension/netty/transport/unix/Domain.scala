package qgame.akka.extension.netty.transport.unix

import akka.actor.{ ExtensionKey, ActorRef, ExtendedActorSystem }
import akka.io.IO.Extension

/**
 * Created by kerr.
 */
object Domain extends ExtensionKey[DomainExt] {

}

class DomainExt(system: ExtendedActorSystem) extends Extension {
  override def manager: ActorRef = ???
}