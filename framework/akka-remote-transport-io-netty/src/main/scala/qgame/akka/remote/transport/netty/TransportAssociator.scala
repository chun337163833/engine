package qgame.akka.remote.transport.netty

import java.net.InetSocketAddress

import akka.actor.{ Actor, ActorLogging, Props }
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportManager.TransportType.Tcp
import qgame.akka.remote.transport.netty.TransportManager.{ Associate, ShutdownAssociator }
import qgame.akka.remote.transport.netty.tcp.TcpTransportAssociator

/**
 * Created by kerr.
 */
private[netty] final class TransportAssociator(setting: Setting) extends Actor with ActorLogging {
  override def receive: Receive = {
    case a: Associate =>
      setting.transportType match {
        case Tcp =>
          val tcpAssociator = context.actorOf(Props.create(classOf[TcpTransportAssociator], setting))
          tcpAssociator ! a
        case _ =>
      }
    case s: ShutdownAssociator =>
      context.children.foreach(_ ! s)
  }
}

object TransportAssociator {
  sealed trait TransportAssociatorEvent
  case class ResolveSocketAddressSuccess(remoteAddress: InetSocketAddress) extends TransportAssociatorEvent
  case class ResolveSocketAddressFailed(cause: Throwable) extends TransportAssociatorEvent

}
