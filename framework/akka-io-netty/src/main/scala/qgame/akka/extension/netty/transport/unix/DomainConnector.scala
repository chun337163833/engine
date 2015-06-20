package qgame.akka.extension.netty.transport.unix

import akka.actor.ActorRef
import io.netty.channel.unix.DomainSocketChannel
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.akka.extension.netty.transport.{ Connect, Connector }

/**
 * Created by kerr.
 */
class DomainConnector(
  connect: Connect,
  connectCommander: ActorRef,
  setting: Setting
) extends Connector[DomainSocketChannel](
  connect, connectCommander, setting
) {
  override protected def setupChannelOptions(ch: DomainSocketChannel): Unit = ???

  override protected def handleOutGoingConnection(channel: DomainSocketChannel): Unit = ???

  override protected def newChannel: DomainSocketChannel = ???
}
