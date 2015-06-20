package qgame.akka.extension.netty.transport

import java.io.IOException
import java.net.InetSocketAddress

import akka.actor._
import akka.util.ByteString
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.{ ChannelInputShutdownEvent, SocketChannel }
import io.netty.util.ReferenceCountUtil
import qgame.akka.extension.netty._
import qgame.akka.extension.netty.transport.CloseCommand.{ Abort, Close, ConfirmedClose }
import qgame.akka.extension.netty.transport.ConnectionClosed._
import qgame.akka.extension.netty.transport.ConnectionHandler.CloseCause.{ HandlerTerminated, Ignore, UserCommand }
import qgame.akka.extension.netty.transport.ConnectionHandler.ConnectionHandlerEvent._
import qgame.akka.extension.netty.transport.ConnectionHandler._
import qgame.akka.extension.netty.transport.WriteCommand.{ Flush, Write, WriteAndFlush, WriteFile }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
abstract class ConnectionHandler[C <: Channel](
    channel: C,
    commandHandler: ActorRef,
    timeout: FiniteDuration,
    setting: Setting,
    pullMode: Boolean = false
) extends Actor with ActorLogging {
  import context.dispatcher

  private var timeoutTask: Cancellable = _
  context.watch(commandHandler)

  protected def notifyConnected(channel: C): Unit = {
    commandHandler ! Connected(channel.remoteAddress(), channel.localAddress())
    import context.dispatcher
    timeoutTask = context.system.scheduler.scheduleOnce(timeout) {
      self ! RegisterTimeOut
    }
    context.become(waitingRegister)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    notifyConnected(channel)
  }

  private def waitingRegister: Receive = {
    //todo back pressure
    case Register(handler, backPressure, autoRelease, autoFlush) =>
      if (timeoutTask ne null) {
        timeoutTask.cancel()
      }
      if (handler != commandHandler) {
        context.unwatch(commandHandler)
        context.watch(handler)
      }
      log.debug("tcp connectionActor registered with handler :[{}],backPressure :[{}],autoRelease :[{}],autoFlush :[{}].", handler, backPressure, autoRelease, autoFlush)
      val p = channel.pipeline()
      if (!channel.pipeline().iterator().hasNext) {
        p.addLast("inBound", new ConnectionChannelInboundHandler0(self, autoRelease, autoFlush))
        p.addLast("outBound", new ConnectionChannelOutBoundHandler)
      } else {
        p.addLast("inBound", new ConnectionChannelInboundHandler(self, autoRelease, autoFlush))
      }
      if (pullMode) {
        channel.config().setAutoRead(false)
      } else {
        channel.config().setAutoRead(true)
      }
      handler ! Active(channel.remoteAddress(), channel.localAddress())
      context.become(channelActive(channel, handler, backPressure) orElse handleChannelException(channel, handler))
    case RegisterTimeOut =>
      log.warning("register timeout after :[{}],will stop tcp connectionActor.", timeout)
      channel.unsafe().closeForcibly()
      context.stop(self)
    case Terminated(ref) =>
      log.warning("commandHandler stopped before socket channel registered.:[{}],handler :[{}]", channel, commandHandler)
      channel.close()
      context.stop(self)
  }

  override def receive: Actor.Receive = {
    case msg =>
  }

  //TODO split pull & push mode?
  private def channelActive(channel: C, handler: ActorRef, backPressure: Boolean): Receive = {
    case m: Received =>
      handler ! m
    case w @ Write(msg) =>
      if (backPressure && !channel.isWritable) {
        sender() ! CommandFailed(w, info = Some("back pressure."))
      } else {
        channel.write(msg)
      }
    case wf @ WriteAndFlush(msg) =>
      if (backPressure && !channel.isWritable) {
        sender() ! CommandFailed(wf, info = Some("back pressure."))
      } else {
        channel.writeAndFlush(msg)
      }
    case Flush =>
      channel.flush()
    case wf @ WriteFile(file, position, count) =>
      try {
        if (backPressure && !channel.isWritable) {
          sender() ! CommandFailed(wf, info = Some("back pressure."))
        } else {
          channel.writeAndFlush(new DefaultFileRegion(file, position, count))
        }
      } catch {
        case NonFatal(e) =>
          handler ! CommandFailed(wf, Some(e))
      }
    /////////////////

    case SuspendReading =>
      if (!pullMode) {
        channel.config().setAutoRead(false)
      }
    case ResumeReading =>
      if (pullMode) {
        channel.read()
      } else {
        channel.config().setAutoRead(true)
      }

    ///////////////////////////////
    case ConnectionInactive(ch) =>
      log.debug("channel inactive,closed by peer. :[{}]", ch)
      handler ! InActive(ch.remoteAddress().asInstanceOf[InetSocketAddress], ch.localAddress().asInstanceOf[InetSocketAddress])
      context.become(whenChannelInactive(channel, handler))
    case cmd: CloseCommand =>
      log.debug("receive close command. :[{}].", cmd)
      handleCloseCommand(cmd, channel, handler)
    case Terminated(ref) =>
      log.warning("handler :[{}] terminated,will stop self.", ref)
      doPendingWrite(HandlerTerminated, channel, handler)
      context.become(whenClosingWithPendingWrite(HandlerTerminated, channel, ref) orElse handleChannelException(channel, handler))
  }

  private def handleCloseCommand(cmd: CloseCommand, chanel: C, handler: ActorRef): Unit = {
    cmd match {
      case ConfirmedClose | Close =>
        log.debug("handle close command :[{}],will do pending write.", cmd)
        doPendingWrite(CloseCause.UserCommand(cmd), channel, handler)
        context.become(whenClosingWithPendingWrite(CloseCause.UserCommand(cmd), channel, handler))
      case Abort =>
        log.debug("handle close command :[{}],will close now.", cmd)
        preAbort(channel)
        doGracefulClose(channel)
        context.become(whenClosingGracefully(UserCommand(cmd), channel, handler))
    }
  }

  protected def preAbort(channel: C): Unit

  private def doPendingWrite(closeCause: CloseCause, channel: C, handler: ActorRef): Unit = {
    def flushing(): Unit = {
      import Netty.NettyFutureBridge._
      if (channel.isActive && channel.isWritable) {
        channel.writeAndFlush(channel.alloc().buffer(0)).onComplete {
          case Success(v) =>
            self ! FlushedAll
          case Failure(e) =>
            self ! FlushFailed(e)
        }
      } else {
        self ! FlushFailed(new IOException("socket channel's output stream have been shutdown."))
      }
    }
    if (channel.isActive) {
      closeCause match {
        case CloseCause.UserCommand(cmd) => cmd match {
          case ConfirmedClose =>
            flushing()
          case Close =>
            flushing()
          case Abort =>
          //ignore
        }
        case CloseCause.PeerClosed =>
          flushing()
        case CloseCause.HandlerTerminated =>
          flushing()
        case _ =>
          self ! FlushedAll
      }
    } else {
      self ! FlushedAll
    }
  }

  //TODO make use of writeablility?
  private def whenClosingWithPendingWrite(cause: CloseCause, channel: C, handler: ActorRef): Receive = {
    case FlushedAll =>
      log.debug("flushed all")
      cause match {
        case CloseCause.UserCommand(cmd) =>
          cmd match {
            case ConfirmedClose =>
              doConfirmedClose(channel)
              context.become(whenConfirmedClosing(channel, handler))
            case Close =>
              doGracefulClose(channel) //for the quick path
              context.become(whenClosingGracefully(cause, channel, handler))
            case Abort =>
            //ignore
          }
        case CloseCause.PeerClosed =>
          doGracefulClose(channel)
          context.become(whenClosingGracefully(cause, channel, handler))
        case CloseCause.HandlerTerminated =>
          doGracefulClose(channel)
          context.become(whenClosingGracefully(cause, channel, handler))
        case CloseCause.SocketChannelError(e) => //ignore
        case CloseCause.Ignore =>
      }
    case FlushFailed(e) =>
      log.warning("try to flushing failed :[{}] for channel :[{}]", e.getMessage, channel)
      doGracefulClose(channel)
      context.become(whenClosingGracefully(cause, channel, handler))
  }

  protected def doConfirmedClose(channel: C): Unit

  private def whenConfirmedClosing(channel: Channel, handler: ActorRef): Receive = {
    case ConnectionOutputShutdownSuccess(ch) =>
      handler ! ConfirmedClosed
      context.become(whenClosingGracefully(CloseCause.UserCommand(ConfirmedClose), channel, handler))
    case ConnectionOutputShutdownFailed(e) =>
      handler ! CommandFailed(ConfirmedClose, Some(e))
      log.error(e, "error when shutting down output stream,closing now.")
      doGracefulClose(channel)
      context.become(whenClosingGracefully(CloseCause.SocketChannelError(e), channel, handler))
  }

  private def doGracefulClose(channel: Channel): Unit = {
    //if channel is not active,just closed it
    import Netty.NettyFutureBridge._
    channel.disconnect().onComplete {
      case Success(ch) =>
        if ((ch ne null) && ch.isOpen) {
          ch.close().onComplete {
            case Success(cch) =>
            case Failure(ce) =>
              channel.unsafe().closeForcibly()
          }
        }
      case Failure(e) =>
        if (channel.isOpen) {
          channel.close().onComplete {
            case Success(cch) =>
            case Failure(ce) =>
              channel.unsafe().closeForcibly()
          }
        }
    }
  }

  private def whenChannelInactive(channel: C, handler: ActorRef): Receive = {
    case ConnectionUnRegistered(ch) =>
      log.debug("channel unregistered,closed by peer :[{}]", ch)
      handler ! PeerClosed
      context.stop(self)
  }
  private def whenClosingGracefully(cause: CloseCause, channel: Channel, handler: ActorRef): Receive = {
    case ConnectionInactive(ich) =>
      if (handler ne null) {
        cause match {
          case Ignore | HandlerTerminated =>
          case UserCommand(cmd) => cmd match {
            case ConfirmedClose =>
            case _ =>
              handler ! InActive(ich.remoteAddress().asInstanceOf[InetSocketAddress], ich.localAddress().asInstanceOf[InetSocketAddress])
          }
          case _ =>
            handler ! InActive(ich.remoteAddress().asInstanceOf[InetSocketAddress], ich.localAddress().asInstanceOf[InetSocketAddress])
        }
      }
    case ConnectionUnRegistered(ch) =>
      log.debug("socket channel :[{}] unregistered.", ch)
      invokeOnSocketChannelUnregistered(cause, channel, handler)
  }

  private def handleChannelException(channel: C, handler: ActorRef): Receive = {
    case ConnectionExceptionCaught(cause) =>
      log.error(cause, "socket channel :[{}] exception,will stop.", channel)
      doGracefulClose(channel)
      context.become(whenClosingGracefully(CloseCause.SocketChannelError(cause), channel, handler))
    case ConnectionPeerClosed(v) =>
      log.warning("socket channel :[{}] peer closed,will stop now.")
      channel.config().setAutoRead(false)
      doPendingWrite(CloseCause.PeerClosed, channel, handler)
      context.become(whenClosingWithPendingWrite(CloseCause.PeerClosed, channel, handler))
  }

  private def invokeOnSocketChannelUnregistered(cause: CloseCause, channel: Channel, handler: ActorRef): Unit = {
    log.debug("invoked on socket channel unregistered :[{}] ,channel :[{}].", cause, channel)
    if (handler ne null) {
      cause match {
        case Ignore | HandlerTerminated =>
        case CloseCause.PeerClosed =>
          handler ! PeerClosed
        case CloseCause.SocketChannelError(closeCause) =>
          handler ! ErrorClosed(closeCause)
        case CloseCause.UserCommand(cmd) =>
          cmd match {
            case Close =>
              handler ! Closed
            case ConfirmedClose =>
            //ignore this line
            // handler ! ConfirmedClosed
            case Abort =>
              handler ! Aborted
          }
      }
      context.unwatch(handler)
    }
    context.stop(self)
  }

}

object ConnectionHandler {
  lazy val sharedNIOEventLoop = new NioEventLoopGroup()
  lazy val sharedEpollEventLoop = new EpollEventLoopGroup()

  case object RegisterTimeOut

  private class ConnectionChannelInboundHandler(connectionActor: ActorRef, autoRelease: Boolean, autoFlush: Boolean) extends ChannelInboundHandlerAdapter {

    override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
      connectionActor ! Received(msg)
      if (autoRelease)
        ReferenceCountUtil.release(msg)
    }

    override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
      if (autoFlush) {
        ctx.flush()
      }
    }

    override def channelInactive(ctx: ChannelHandlerContext): Unit = {
      connectionActor ! ConnectionInactive(ctx.channel())
    }

    override def channelUnregistered(ctx: ChannelHandlerContext): Unit = {
      connectionActor ! ConnectionUnRegistered(ctx.channel())
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      cause match {
        case ioe: IOException =>
          //connection reset by peer
          if (ctx.channel().isOpen && ctx.channel().asInstanceOf[SocketChannel].config().isAllowHalfClosure) {
            //ignore,will follow one
          } else {
            connectionActor ! ConnectionExceptionCaught(cause)
          }
        case e =>
          connectionActor ! ConnectionExceptionCaught(cause)
      }
    }

    override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
      evt match {
        case e: ChannelInputShutdownEvent =>
          //peer closed
          connectionActor ! ConnectionPeerClosed(ctx.channel())
        case other =>
          super.userEventTriggered(ctx, evt)
      }
    }

  }

  class ConnectionChannelOutBoundHandler extends ChannelOutboundHandlerAdapter {
    override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
      msg match {
        case bs: ByteString =>
          ctx.write(ctx.alloc().buffer(bs.size).writeBytes(bs.asByteBuffer), promise)
        case _ => ctx.write(msg, promise)
      }
    }
  }

  private class ConnectionChannelInboundHandler0(connector: ActorRef, autoRelease: Boolean, autoFlush: Boolean)
      extends ConnectionChannelInboundHandler(connector, autoRelease, autoFlush) {
    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
      val byteBuf = msg.asInstanceOf[ByteBuf]
      val bytes = new Array[Byte](byteBuf.readableBytes())
      byteBuf.readBytes(bytes)
      connector ! Received(ByteString.apply(bytes))
      ReferenceCountUtil.release(msg)
    }
  }

  sealed trait ConnectionHandlerEvent
  object ConnectionHandlerEvent {

    case class ConnectionExceptionCaught(cause: Throwable) extends ConnectionHandlerEvent

    case class ConnectionPeerClosed(channel: Channel) extends ConnectionHandlerEvent

    case class ConnectionOutputShutdownSuccess(channel: Channel) extends ConnectionHandlerEvent

    case class ConnectionOutputShutdownFailed(cause: Throwable) extends ConnectionHandlerEvent

    case class ConnectionInactive(channel: Channel) extends ConnectionHandlerEvent

    case class ConnectionUnRegistered(channel: Channel) extends ConnectionHandlerEvent

    case object FlushedAll extends ConnectionHandlerEvent

    case class FlushFailed(cause: Throwable) extends ConnectionHandlerEvent
  }

  sealed trait CloseCause

  object CloseCause {

    case object Ignore extends CloseCause

    case object PeerClosed extends CloseCause

    case object HandlerTerminated extends CloseCause

    case class SocketChannelError(cause: Throwable) extends CloseCause

    case class UserCommand(cmd: CloseCommand) extends CloseCause

  }
}
