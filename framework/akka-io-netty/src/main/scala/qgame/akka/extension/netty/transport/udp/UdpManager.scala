package qgame.akka.extension.netty.transport.udp

import java.net.InetSocketAddress

import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.akka.extension.netty.transport.{ Bind, Connect, TransportManager }

/**
 * Created by kerr.
 */
class UdpManager(setting: Setting) extends TransportManager[InetSocketAddress](setting, classOf[InetSocketAddress]) {
  override protected def handleBind(bind: Bind, bindAddress: InetSocketAddress): Unit = ???

  override protected def handleConnect(connect: Connect, localAddress: Option[InetSocketAddress], remoteAddress: InetSocketAddress): Unit = ???
}
