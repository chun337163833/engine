package qgame.akka.extension.netty.transport

import java.net.{ InetSocketAddress, SocketAddress }

import akka.actor.{ Actor, ActorLogging, ActorRef, Terminated }
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.{ Channel, EventLoopGroup }
import io.netty.resolver.DefaultNameResolverGroup
import io.netty.util.concurrent.Future
import qgame.akka.extension.netty.Netty
import qgame.akka.extension.netty.Netty.{ ExecutorGroupStage, InvokerStage, SimpleStage }
import qgame.akka.extension.netty.transport.Connector.ConnectorChannelEvent._
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
abstract class Connector[C <: Channel](
    connect: Connect,
    connectCommander: ActorRef,
    setting: Setting
) extends Actor with ActorLogging with ChannelBootStrap[C] {
  import Netty.NettyFutureBridge._
  import context.dispatcher

  val Connect(connectHandler, remoteAddress, localAddress, registerTimeOut, options, pipeline, pullMode, shared) = connect
  context.watch(connectCommander)

  override protected def eventLoopGroup: EventLoopGroup = {
    if (shared) {
      if (setting.native) Connector.sharedEpollEventLoopGroup else Connector.sharedEventLoopGroup
    } else {
      if (setting.native) new EpollEventLoopGroup() else new NioEventLoopGroup()
    }
  }
  protected val connectorChannel: C = newChannel

  protected def setupChannelOptions(ch: C): Unit

  override protected def init(): Unit = {
    val p = connectorChannel.pipeline()
    pipeline.foreach {
      case SimpleStage(name, channelHandler) =>
        p.addLast(name, channelHandler())
      case InvokerStage(invoker, name, channelHandler) =>
        p.addLast(invoker, name, channelHandler())
      case ExecutorGroupStage(executor, name, channelHandler) =>
        p.addLast(executor, name, channelHandler())
    }
    setupChannelOptions(connectorChannel)
    //turn off auto read
    newChannel.config().setAutoRead(false)
  }

  protected val connectorEventLoopGroup: EventLoopGroup = eventLoopGroup

  override protected def register(): Unit = {
    try {
      connectorEventLoopGroup.register(connectorChannel).onComplete {
        case Success(v) =>
          self ! ConnectorChannelRegisterSuccess(connectorChannel)
        case Failure(e) =>
          self ! ConnectorChannelRegisterFailed(connectorChannel, e)
      }
    } catch {
      case NonFatal(e) =>
        self ! ConnectorChannelRegisterFailed(connectorChannel, e)
      case e: Throwable =>
        connectorChannel.unsafe().closeForcibly()
        throw e
    }
  }

  override protected def shutdown(): Unit = {
    if (!shared) {
      connectorEventLoopGroup.shutdownGracefully()
    }
  }

  init()
  register()

  override def receive: Receive = {
    case ConnectorChannelRegisterSuccess(ch) =>
      log.debug("socket channel :[{}] registered to event loop,going to connect to remote :[{}] from :[{}].", ch, remoteAddress, localAddress)
      val resolver = Connector.resolver
      if (!resolver.isSupported(remoteAddress)) {
        log.debug("unsupported remote address :[{}] for resolver,try connect directly.", remoteAddress)
        doConnect(ch.asInstanceOf[C], remoteAddress)
      } else if (resolver.isResolved(remoteAddress)) {
        val resolvedRemoteAddr = resolver.resolve(remoteAddress).getNow
        log.debug("remote address :[{}] have been resolved,as :[{}],connect now.", remoteAddress, resolvedRemoteAddr)
        doConnect(ch.asInstanceOf[C], resolvedRemoteAddr)
      } else {
        log.debug("remote address :[{}] have not been resolved,trying to resolving now.", remoteAddress)
        resolver.resolve(remoteAddress).onComplete {
          case Success(resolvedRemoteAddress) =>
            self ! RealRemoteAddressResolveSuccess(ch, remoteAddress.asInstanceOf[InetSocketAddress], resolvedRemoteAddress)
          case Failure(e) =>
            self ! RealRemoteAddressResolveFailed(ch, remoteAddress.asInstanceOf[InetSocketAddress], e)
        }
      }
    case RealRemoteAddressResolveSuccess(ch, remoteAddr, realRemoteAddr) =>
      log.debug("remote address resolved for :[{}],real :[{}].", remoteAddr, realRemoteAddr)
      doConnect(ch.asInstanceOf[C], realRemoteAddr)
    case RealRemoteAddressResolveFailed(ch, remoteAddr, e) =>
      log.error(e, "remote address resolve failed for :[{}],will stop.", remoteAddr)
      ch.close().onFailure {
        case _ =>
          ch.unsafe().closeForcibly()
      }
      context.stop(self)
    case ConnectorChannelRegisterFailed(ch, e) =>
      log.error(e, "socket channel :[{}] register to event loop failed,going to stop.", ch)
      ch.unsafe().closeForcibly()
      context.stop(self)
  }

  protected def doConnect(ch: C, remoteAddress: SocketAddress): Unit = {
    val future = {
      localAddress match {
        case Some(localAddr) =>
          ch.connect(remoteAddress, localAddr)
        case None =>
          ch.connect(remoteAddress)
      }
    }

    future.onComplete {
      case Success(v) =>
        self ! ConnectorChannelConnectSuccess(v)
      case Failure(e) =>
        self ! ConnectorChannelConnectFailed(newChannel, e)
    }
    context.become(waitingConnected(newChannel))
  }

  private def waitingConnected(channel: Channel): Receive = {
    case ConnectorChannelConnectSuccess(ch) =>
      log.debug("socket channel :[{}] connected,start to handle with out going tcp connection handler.", ch)

      //      val outGoingConnectionHandler = context.actorOf(OutGoingTcpConnectionHandler.props(
      //        ch,
      //        connectHandler,
      //        registerTimeOut,
      //        setting,
      //        pullMode
      //      ))
      //
      //      context.watch(outGoingConnectionHandler)
      handleOutGoingConnection(ch.asInstanceOf[C])
      context.become(connected(ch.asInstanceOf[C]))
    case ConnectorChannelConnectFailed(ch, e) =>
      log.error("tcp connector connect from [{}] to remote :[{}] failed.", localAddress, remoteAddress)
      connectCommander ! CommandFailed(connect, Option(e), Option(s"failed to connect to :[$remoteAddress],cause :[${e.getMessage}]"))
      channel.close().onFailure {
        case _ =>
          channel.unsafe().closeForcibly()
      }
      context.stop(self)
    case ConnectorChannelExceptionCaught(ch, e) =>
      log.error(e, "tcp connector exception caught. at ch :[{}]", ch)
      if (ch ne null) {
        import Netty.NettyFutureBridge._
        ch.close().onFailure {
          case _ =>
            ch.unsafe().closeForcibly()
        }
      }
      context.stop(self)
  }

  protected def handleOutGoingConnection(channel: C): Unit

  private def connected(channel: C): Receive = {
    case Terminated(ref) =>
      log.debug("out going connection handler :[{}] stopped,channel active :[{}],terminate self.", ref, channel.isActive)
      if (channel.isActive) {
        channel.close()
      }
      context.stop(self)
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    super.postStop()
    shutdown()
  }

}

object Connector {
  lazy val sharedEventLoopGroup = new NioEventLoopGroup()

  lazy val sharedEpollEventLoopGroup = new EpollEventLoopGroup()

  val resolverGroup = DefaultNameResolverGroup.INSTANCE

  def resolve(inetAddress: String, inetPort: Int): Future[InetSocketAddress] = {
    resolverGroup.getResolver(sharedEventLoopGroup.next())
      .resolve(inetAddress, inetPort)
  }

  def resolver = resolverGroup.getResolver(sharedEventLoopGroup.next())

  sealed trait ConnectorChannelEvent

  object ConnectorChannelEvent {
    case class ConnectorChannelRegisterSuccess(channel: Channel) extends ConnectorChannelEvent

    case class ConnectorChannelRegisterFailed(channel: Channel, cause: Throwable) extends ConnectorChannelEvent

    case class RealRemoteAddressResolveSuccess(channel: Channel, remoteAddress: InetSocketAddress, realRemoteAddress: InetSocketAddress) extends ConnectorChannelEvent

    case class RealRemoteAddressResolveFailed(channel: Channel, remoteAddress: InetSocketAddress, cause: Throwable) extends ConnectorChannelEvent

    case class ConnectorChannelExceptionCaught(channel: Channel, cause: Throwable) extends ConnectorChannelEvent

    case class ConnectorChannelConnectSuccess(channel: Channel) extends ConnectorChannelEvent

    case class ConnectorChannelConnectFailed(channel: Channel, cause: Throwable) extends ConnectorChannelEvent
  }
}
