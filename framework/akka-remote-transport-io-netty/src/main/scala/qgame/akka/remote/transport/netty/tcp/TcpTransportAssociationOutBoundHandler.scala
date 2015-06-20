package qgame.akka.remote.transport.netty.tcp

import akka.actor.{ ActorRef, Props }
import akka.remote.transport.AssociationHandle
import qgame.akka.remote.transport.netty.IONettyTransport.Setting

import scala.concurrent.Promise

/**
 * Created by kerr.
 */
private[netty] final class TcpTransportAssociationOutBoundHandler(
    setting: Setting,
    connectionActor: ActorRef,
    promise: Promise[AssociationHandle]
) extends TcpTransportAssociationHandler(setting, connectionActor) {
  override protected def beforeReadHandlerRegistered(handle: AssociationHandle): Unit = {
    promise.trySuccess(handle)
  }

  override protected def connectionActorTerminatedBeforeRegistered(ref: ActorRef): Unit = {
    promise.tryFailure(new IllegalStateException(s"connection actor :[$ref] terminated before registered."))
  }
}

private[netty] object TcpTransportAssociationOutBoundHandler {
  def props(
    setting: Setting,
    connectionActor: ActorRef,
    promise: Promise[AssociationHandle]
  ): Props =
    Props.create(classOf[TcpTransportAssociationOutBoundHandler], setting, connectionActor, promise)
}
