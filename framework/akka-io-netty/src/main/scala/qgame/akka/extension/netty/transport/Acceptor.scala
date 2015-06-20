package qgame.akka.extension.netty.transport

import java.util.concurrent.TimeUnit

import akka.actor.{ Actor, ActorLogging, ActorRef }
import io.netty.channel._
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import qgame.akka.extension.netty.Netty
import qgame.akka.extension.netty.transport.Acceptor.AcceptorChannelHandler
import qgame.akka.extension.netty.transport.Acceptor.AcceptorChannelEvent._
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
abstract class Acceptor[C <: Channel](
  bind: Bind,
  bindCommander: ActorRef,
  setting: Setting
) extends Actor with ActorLogging
    with ChannelBootStrap[C] {

  import Netty.NettyFutureBridge._
  import context.dispatcher

  val Bind(bindHandler, localAddress, registerTimeOut, options, childOptions, pipeline, pullMode, shared) = bind
  //TODO handle death pack
  context.watch(bindHandler)

  override protected def eventLoopGroup: EventLoopGroup = {
    if (shared) {
      if (setting.native) Acceptor.sharedEpollEventLoop else Acceptor.sharedNIOEventLoop
    } else {
      if (setting.native) new EpollEventLoopGroup() else new NioEventLoopGroup()
    }
  }

  protected val acceptorChannel: C = newChannel

  protected def setupChannelOptions(ch: C): Unit

  override protected def init(): Unit = {
    acceptorChannel.pipeline().addLast("acceptor", new AcceptorChannelHandler(self))
    setupChannelOptions(acceptorChannel)
    //turn off auto accept
    acceptorChannel.config().setAutoRead(false)
  }

  protected val acceptorEventLoopGroup: EventLoopGroup = eventLoopGroup

  override protected def register(): Unit = {
    try {
      acceptorEventLoopGroup.register(acceptorChannel).onComplete {
        case Success(v) =>
          self ! AcceptorChannelRegisterSuccess(v)
        case Failure(e) =>
          self ! AcceptorChannelRegisterFailed(acceptorChannel, e)
      }
    } catch {
      case NonFatal(e) =>
        self ! AcceptorChannelRegisterFailed(acceptorChannel, e)
      case e: Throwable =>
        acceptorChannel.unsafe().closeForcibly()
        throw e
    }
  }

  override protected def shutdown(): Unit = {
    if (!shared) {
      acceptorEventLoopGroup.shutdownGracefully()
    }
  }

  init()
  register()

  override def receive: Receive = {
    case AcceptorChannelRegisterSuccess(ch) =>
      log.debug("server socket channel :[{}] registered to event loop.", ch)
      ch.bind(localAddress).onComplete {
        case Success(v) =>
          self ! AcceptorChannelBound(v)
        case Failure(e) =>
          self ! AcceptorChannelBindFailed(e)
      }
      context.become(waitingBound(ch))
    case AcceptorChannelRegisterFailed(ch, e) =>
      log.error(e, "server socket channel :[{}] registered failed,will stop.", ch)
      bindCommander ! CommandFailed(bind, Option(e), info = Option("failed to register to event loop."))
      ch.unsafe().closeForcibly()
      context.stop(self)
  }

  private def waitingBound(channel: Channel): Receive = {
    case AcceptorChannelBound(ch) =>
      log.debug("server socket channel :[{}] bound.pullMode :[{}]", ch, pullMode)
      bindCommander ! Bound(channel.asInstanceOf[ServerSocketChannel].localAddress())
      if (pullMode) {
        channel.config().setAutoRead(false)
        context.become(pullModeBound(channel.asInstanceOf[C], 0))
      } else {
        channel.config().setAutoRead(true)
        channel.read()
        context.become(pushModeBound(channel.asInstanceOf[C]))
      }
    case AcceptorChannelBindFailed(e) =>
      log.error(e, "server socket channel failed to bind at :[{}] failed,will stop.", localAddress)
      bindCommander ! CommandFailed(bind, Option(e), info = Option(s"bind failed ,cause :[${e.getMessage}]"))
      channel.close().onFailure {
        case _ =>
          channel.unsafe().closeForcibly()
      }
      context.stop(self)
  }

  protected def handleIncomingConnection(childChannel: Channel)

  private def pullModeBound(channel: C, acceptSize: Long, paused: Boolean = false): Receive = {
    case ChannelRegistrable(childChannel) =>
      handleIncomingConnection(childChannel)
      val newAcceptSize = acceptSize - 1
      context.become(pullModeBound(channel, newAcceptSize))
      if (newAcceptSize > 0 && !paused) {
        channel.read()
      }
    case SuspendAccepting =>
      context.become(pullModeBound(channel, acceptSize, paused = true))
    case r @ ResumeAccepting(batchSize) =>
      if (batchSize >= 0) {
        context.become(pullModeBound(channel, acceptSize + batchSize, paused = false))
        if (paused) {
          channel.read()
        }
      } else {
        sender() ! CommandFailed(r)
      }
    case AcceptorChannelExceptionCaught(e) =>
      log.error(e, "server socket channel exception caught,will return after 1 second pause,don't worry.")
    case Unbind =>
      log.debug("handle the unbind command from :[{}]", sender())
      context.stop(self)
  }

  private def pushModeBound(channel: C): Receive = {
    case ChannelRegistrable(childChannel) =>
      handleIncomingConnection(childChannel)
    case SuspendAccepting =>
      channel.config().setAutoRead(false)
    case ResumeAccepting(_) =>
      channel.config().setAutoRead(true)
    case AcceptorChannelExceptionCaught(e) =>
      log.error(e, "server socket channel exception caught,will return after 1 second pause,don't worry.")
    case Unbind =>
      log.debug("handle the unbind command from :[{}]", sender())
      context.stop(self)
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    super.postStop()
    shutdown()
  }

}

object Acceptor {
  lazy val sharedNIOEventLoop = new NioEventLoopGroup()
  lazy val sharedEpollEventLoop = new EpollEventLoopGroup()

  private class AcceptorChannelHandler(acceptor: ActorRef) extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
      val childChannel = msg.asInstanceOf[Channel]
      acceptor ! ChannelRegistrable(childChannel)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      val config: ChannelConfig = ctx.channel.config
      if (config.isAutoRead) {
        config.setAutoRead(false)
        ctx.channel().eventLoop.schedule(new Runnable() {
          def run() {
            config.setAutoRead(true)
          }
          //TODO make heal time configurable
        }, 1, TimeUnit.SECONDS)
      }
    }

  }

  sealed trait AcceptorChannelEvent

  object AcceptorChannelEvent {

    case class AcceptorChannelRegisterSuccess(channel: Channel) extends AcceptorChannelEvent

    case class AcceptorChannelRegisterFailed(channel: Channel, cause: Throwable) extends AcceptorChannelEvent

    case class AcceptorChannelBound(channel: Channel) extends AcceptorChannelEvent

    case class AcceptorChannelBindFailed(e: Throwable) extends AcceptorChannelEvent

    case class ChannelRegistrable(channel: Channel) extends AcceptorChannelEvent

    case class AcceptorChannelExceptionCaught(cause: Throwable) extends AcceptorChannelEvent
  }

}
