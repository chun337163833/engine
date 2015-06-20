package qgame.akka.remote.transport.netty.tcp

import akka.actor._
import akka.remote.transport.AssociationHandle
import akka.remote.transport.Transport.{ InboundAssociation, AssociationEventListener }
import qgame.akka.remote.transport.netty.IONettyTransport.Setting

/**
 * Created by kerr.
 */
private[netty] final class TcpTransportAssociationInboundHandler(
    setting: Setting,
    connectionActor: ActorRef,
    listener: AssociationEventListener
) extends TcpTransportAssociationHandler(setting, connectionActor) {
  override protected def beforeReadHandlerRegistered(handle: AssociationHandle): Unit = {
    listener.notify(InboundAssociation(handle))
  }

  override protected def connectionActorTerminatedBeforeRegistered(ref: ActorRef): Unit = {
    //NOOP
  }
}

private[netty] object TcpTransportAssociationInboundHandler {
  def props(
    setting: Setting,
    connectionActor: ActorRef,
    listener: AssociationEventListener
  ): Props =
    Props.create(classOf[TcpTransportAssociationInboundHandler], setting, connectionActor, listener)
}
