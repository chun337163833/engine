package qgame.akka.extension.netty.transport.udp

import akka.actor.ActorRef
import io.netty.channel.socket.DatagramChannel
import qgame.akka.extension.netty.transport.{ Connector, Connect }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

/**
 * Created by kerr.
 */
class UdpConnector(
    connect: Connect,
    connectCommander: ActorRef,
    setting: Setting
) extends Connector[DatagramChannel](connect, connectCommander, setting) {
  override protected def setupChannelOptions(ch: DatagramChannel): Unit = ???

  override protected def handleOutGoingConnection(channel: DatagramChannel): Unit = ???

  override protected def newChannel: DatagramChannel = ???
}
