package qgame.akka.extension.netty.transport.udp

import akka.actor.ActorRef
import io.netty.channel.Channel
import io.netty.channel.socket.DatagramChannel
import qgame.akka.extension.netty.transport.{ Acceptor, Bind }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

/**
 * Created by kerr.
 */
class UdpAcceptor(bind: Bind, bindCommander: ActorRef, setting: Setting)
    extends Acceptor[DatagramChannel](bind, bindCommander, setting) {
  override protected def setupChannelOptions(ch: DatagramChannel): Unit = ???

  override protected def handleIncomingConnection(childChannel: Channel): Unit = ???

  override protected def newChannel: DatagramChannel = ???
}
