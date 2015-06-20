package qgame.akka.remote.transport.netty

import akka.actor.{ Actor, ActorLogging, Props }
import akka.remote.transport.AssociationHandle.HandleEventListener
import akka.remote.transport.Transport.AssociationEventListener
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportManager.TransportType.Tcp
import qgame.akka.remote.transport.netty.TransportManager.{ Listen, ShutdownListener }
import qgame.akka.remote.transport.netty.tcp.TcpTransportListener

/**
 * Created by kerr.
 */
private[netty] final class TransportListener(setting: Setting) extends Actor with ActorLogging {
  override def receive: Receive = {
    case l: Listen =>
      setting.transportType match {
        case Tcp =>
          val tcpListener = context.actorOf(Props.create(classOf[TcpTransportListener], setting), "tcp")
          tcpListener ! l
        case _ =>
      }
    case s: ShutdownListener =>
      context.children.foreach(_ ! s)
  }

}

private[netty] object TransportListener {
  sealed trait TransportListenerEvent
  case class AssociationEventListenerRegisterSuccess(listener: AssociationEventListener) extends TransportListenerEvent
  case class AssociationEventListenerRegisterFailed(cause: Throwable) extends TransportListenerEvent

  case class ReadHandleEventListenerRegisterSuccess(listener: HandleEventListener) extends TransportListenerEvent
  case class ReadHandleEventListenerRegisterFailed(cause: Throwable) extends TransportListenerEvent

}
