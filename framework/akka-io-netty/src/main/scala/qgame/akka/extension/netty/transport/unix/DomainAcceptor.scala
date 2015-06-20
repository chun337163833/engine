package qgame.akka.extension.netty.transport.unix

import akka.actor.ActorRef
import io.netty.channel.Channel
import io.netty.channel.unix.ServerDomainSocketChannel
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.akka.extension.netty.transport.{ Acceptor, Bind }

/**
 * Created by kerr.
 */
final class DomainAcceptor(bind: Bind, bindCommander: ActorRef, setting: Setting)
    extends Acceptor[ServerDomainSocketChannel](bind, bindCommander, setting) {
  override protected def setupChannelOptions(ch: ServerDomainSocketChannel): Unit = ???

  override protected def handleIncomingConnection(childChannel: Channel): Unit = ???

  override protected def newChannel: ServerDomainSocketChannel = ???
}
