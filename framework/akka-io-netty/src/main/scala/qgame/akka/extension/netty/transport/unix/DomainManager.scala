package qgame.akka.extension.netty.transport.unix

import io.netty.channel.unix.DomainSocketAddress
import qgame.akka.extension.netty.transport.{ Bind, Connect, TransportManager }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

/**
 * Created by kerr.
 */
final case class DomainManager(setting: Setting) extends TransportManager[DomainSocketAddress](setting, classOf[DomainSocketAddress]) {
  override protected def handleBind(
    bind: Bind,
    bindAddress: DomainSocketAddress
  ): Unit = {
    log.debug("going bind at domain address [{}]", bindAddress)

  }

  override protected def handleConnect(
    connect: Connect,
    localAddress: Option[DomainSocketAddress],
    remoteAddress: DomainSocketAddress
  ): Unit = ???
}
