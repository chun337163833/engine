package qgame.akka.remote.transport.netty.tcp

import java.net.InetSocketAddress

import akka.actor._
import akka.remote.transport.AssociationHandle
import akka.remote.transport.AssociationHandle.{ Disassociated, HandleEventListener, InboundPayload }
import akka.util.ByteString
import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCountUtil
import qgame.akka.extension.netty.transport.CloseCommand.Close
import qgame.akka.extension.netty.transport.WriteCommand.WriteAndFlush
import qgame.akka.extension.netty.transport._
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportListener.{ ReadHandleEventListenerRegisterFailed, ReadHandleEventListenerRegisterSuccess }

import scala.concurrent.Promise
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
private[netty] abstract class TcpTransportAssociationHandler(setting: Setting, connectionActor: ActorRef) extends Actor with Stash with ActorLogging {
  import context.dispatcher

  context.watch(connectionActor)
  connectionActor ! Register(self, backPressure = false, autoRelease = false, autoFlush = false)

  override def receive: Receive = {
    case Active(remoteAddress, localAddress) =>
      log.debug("tcp transport connection active remote :[{}],local :[{}]", remoteAddress, localAddress)
      val remoteAddr = remoteAddress.asInstanceOf[InetSocketAddress]
      val localAddr = localAddress.asInstanceOf[InetSocketAddress]
      println("--------------------------------")
      println(setting.transportProtocal)
      println(context.system.name)
      println(remoteAddr.getHostString)
      println(remoteAddr.getPort)
      //NEP again
      println("--------------------------------")
      val remote = Address(
        setting.transportProtocal,
        context.system.name,
        remoteAddr.getHostString,
        remoteAddr.getPort
      )
      val local = Address(setting.transportProtocal, context.system.name, localAddr.getHostString, localAddr.getPort)

      val handle = new TcpTransportAssociationHandle(sender(), remote, local)
      handle.readHandlerPromise.future.onComplete {
        case Success(readListener) =>
          self ! ReadHandleEventListenerRegisterSuccess(readListener)
        case Failure(e) =>
          self ! ReadHandleEventListenerRegisterFailed(e)
      }
      beforeReadHandlerRegistered(handle)
      context.become(waitingRegisterReadHandle(remote, local, handle))
    case Terminated(ref) =>
      log.debug("connection actor terminated before registered :[{}]", ref)
      connectionActorTerminatedBeforeRegistered(ref)
      context.stop(self)
  }

  protected def connectionActorTerminatedBeforeRegistered(ref: ActorRef): Unit

  protected def beforeReadHandlerRegistered(handle: AssociationHandle): Unit

  private def waitingRegisterReadHandle(remote: Address, local: Address, handle: AssociationHandle): Receive = {
    case ReadHandleEventListenerRegisterSuccess(readListener) =>
      log.debug("read handle event listener registered,remote: [{}],local: [{}]", remote, local)
      unstashAll()
      context.become(readHandlerRegistered(readListener, handle))
    case Received(msg) =>
      log.warning("received msg :[{}] while waiting register read handle,stash it.", msg)
      stash()
    case ReadHandleEventListenerRegisterFailed(e) =>
      log.error(e, "read handle event listener register failed,remote :[{}],local :[{}],disassociate it.", remote, local)
      handle.disassociate()
      context.stop(self)
    case Terminated(ref) =>
      log.error("connection actor terminated when waiting register read handle.ref :[{}]", ref)
      context.stop(self)
  }

  private def readHandlerRegistered(readListener: HandleEventListener, handle: AssociationHandle): Receive = {
    case Received(msg) =>
      msg match {
        case buf: ByteBuf =>
          if (buf.readableBytes() > 0) {
            val bytes = new Array[Byte](buf.readableBytes())
            buf.readBytes(bytes)
            readListener.notify(InboundPayload(ByteString(bytes)))
          }
          ReferenceCountUtil.release(buf)
        case unhandled =>
          log.warning("unhandled msg :[{}]", unhandled)
      }
    case InActive(remoteAddress, localAddress) =>
      log.warning("association inactive,remote :[{}] local :[{}],will stop.", remoteAddress, localAddress)
      readListener.notify(Disassociated(AssociationHandle.Unknown))
      context.stop(self)
    case Terminated(ref) =>
      log.error("association underlying connection terminated :[{}] will stop.", ref)
      readListener.notify(Disassociated(AssociationHandle.Unknown))
      context.stop(self)
  }

  private class TcpTransportAssociationHandle(
      val channelActor: ActorRef,
      val remoteAddress: Address,
      val localAddress: Address
  ) extends AssociationHandle {
    override val readHandlerPromise: Promise[HandleEventListener] = Promise[HandleEventListener]()

    override def disassociate(): Unit = channelActor ! Close

    override def write(payload: ByteString): Boolean = {
      //just return true for the auto scale buffer on the netty side
      //TODO need optimize
      channelActor ! WriteAndFlush(payload)
      true
    }

  }

}

private[netty] object TcpTransportAssociationHandler {
  sealed trait TcpTransportAssociationHandlerEvent
  case class ReadHandleEventListenerRegisterSuccess(listener: HandleEventListener) extends TcpTransportAssociationHandlerEvent
  case class ReadHandleEventListenerRegisterFailed(cause: Throwable) extends TcpTransportAssociationHandlerEvent
}
