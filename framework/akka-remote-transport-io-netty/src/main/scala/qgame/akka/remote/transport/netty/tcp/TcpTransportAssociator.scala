package qgame.akka.remote.transport.netty.tcp

import java.net.{ SocketAddress, InetSocketAddress }

import akka.actor.{ Actor, ActorLogging, ActorRef, Address }
import akka.io.IO
import akka.remote.transport.AssociationHandle
import qgame.akka.extension.netty.transport.CloseCommand.Close
import qgame.akka.extension.netty.transport.tcp.Tcp
import qgame.akka.extension.netty.transport.{ CommandFailed, Connect, Connected }
import qgame.akka.remote.transport.netty.IONettyTransport
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportAssociator.{ ResolveSocketAddressFailed, ResolveSocketAddressSuccess }
import qgame.akka.remote.transport.netty.TransportManager.{ ShutdownTransport, Associate }
import qgame.akka.remote.transport.netty.tcp.TcpTransportAssociator.AssociationException

import scala.concurrent.Promise
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
private[netty] final class TcpTransportAssociator(setting: Setting) extends Actor with ActorLogging {
  import context.{ dispatcher, system }
  private val manager = IO(Tcp)
  override def receive: Receive = {
    case Associate(remote, promise) =>
      log.debug("request to associate to remote :[{}],resolving socket address.", remote)
      IONettyTransport.addressToSocketAddress(remote).onComplete {
        case Success(remoteSocketAddress) =>
          self ! ResolveSocketAddressSuccess(remoteSocketAddress)
        case Failure(e) =>
          self ! ResolveSocketAddressFailed(e)
      }
      context.become(resolvingSocketAddress(remote, promise))
  }

  private def resolvingSocketAddress(remote: Address, promise: Promise[AssociationHandle]): Receive = {
    case ResolveSocketAddressSuccess(remoteSocketAddress) =>
      log.debug("socket address for remote :[{}] resolved at :[{}],associating", remote, remoteSocketAddress)
      manager ! Connect(self, remoteSocketAddress, pipeline = setting.Tcp.stage, shared = true)
      context.become(waitingConnected(remote, remoteSocketAddress, promise))
    case ResolveSocketAddressFailed(e) =>
      log.error(e, "resolve socket address for remote :[{}] failed,stop.", remote)
      promise.tryFailure(e)
      context.stop(self)
  }

  private def waitingConnected(remote: Address, remoteSocketAddress: InetSocketAddress, promise: Promise[AssociationHandle]): Receive = {
    case Connected(remoteAddress, localAddress) =>
      log.debug("tcp transport associator connected to remote address :[{}] from :[{}] for :[{}]", remoteAddress, localAddress, remote)
      context.actorOf(TcpTransportAssociationOutBoundHandler.props(setting, sender(), promise))
      context.become(connected(sender(), remoteAddress, localAddress))
    case CommandFailed(connect, cause, info) =>
      cause match {
        case Some(c) =>
          log.error(c, "tcp transport associator connect to remote address :[{}] for :[{}] failed,cause :[{}],info :[{}]", remoteSocketAddress, remote, cause, info)
          promise.tryFailure(new AssociationException(c))
        case None =>
          log.error("tcp transport associator connect to remote address :[{}] for :[{}] failed,cause :[{}],info :[{}]", remoteSocketAddress, remote, cause, info)
          promise.tryFailure(new AssociationException(new RuntimeException(s"could not connect to remote address :[$remoteSocketAddress] for [$remote] ")))
      }
      context.stop(self)
  }

  private def connected(connectionActor: ActorRef, remoteAddress: SocketAddress, localAddress: SocketAddress): Receive = {
    case ShutdownTransport(promise) =>
      log.debug("shutdown transport the connector side,from :[{}] to :[{}]", remoteAddress, localAddress)
      connectionActor ! Close
      promise.trySuccess(true)
      context.stop(self)
  }

}

private[netty] object TcpTransportAssociator {
  class AssociationException(cause: Throwable) extends RuntimeException(cause)
}
