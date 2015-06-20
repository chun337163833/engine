package qgame.akka.extension.netty.transport.tcp

import akka.actor.{ ActorRef, Props }
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ Channel, EventLoopGroup }
import qgame.akka.extension.netty.Netty
import qgame.akka.extension.netty.Netty.Stage
import qgame.akka.extension.netty.transport.ConnectionHandler.ConnectionHandlerEvent.{ ConnectionOutputShutdownFailed, ConnectionOutputShutdownSuccess }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.akka.extension.netty.transport.{ ChannelOption, InComingConnectionHandler }

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
class InComingTcpConnectionHandler(
  eventLoopGroup: EventLoopGroup,
  channel: SocketChannel,
  commandHandler: ActorRef,
  timeout: FiniteDuration,
  options: immutable.Traversable[ChannelOption[Channel]] = Nil,
  pipeline: immutable.Traversable[Stage] = Nil,
  setting: Setting,
  pullMode: Boolean
) extends InComingConnectionHandler[SocketChannel](
  eventLoopGroup, channel, commandHandler, timeout, options, pipeline, setting, pullMode
) {
  override protected def preAbort(channel: SocketChannel): Unit = {
    channel.config().setSoLinger(0)
  }

  override protected def doConfirmedClose(channel: SocketChannel): Unit = {
    if (!channel.isOutputShutdown) {
      import Netty.NettyFutureBridge._
      import context.dispatcher
      channel.shutdownOutput().onComplete {
        case Success(v) =>
          self ! ConnectionOutputShutdownSuccess(v)
        case Failure(e) =>
          self ! ConnectionOutputShutdownFailed(e)
      }
    } else {
      self ! ConnectionOutputShutdownSuccess(channel)
    }
  }

  override protected def setupChildChannelOptions(childChannel: SocketChannel): Unit = {
    setting.Connection.options.foreach(_(channel))
    options.foreach(_(channel))

    channel match {
      case ch: EpollSocketChannel =>
        setting.EpollConnection.options.foreach(_(ch))
      //        options.collect {
      //          case op: EpollChannelOption => op
      //        }.foreach(_(ch))
      case _ =>
    }
  }
}

object InComingTcpConnectionHandler {
  def props(
    eventLoopGroup: EventLoopGroup,
    channel: SocketChannel,
    commandHandler: ActorRef,
    timeout: FiniteDuration,
    options: immutable.Traversable[ChannelOption[Channel]] = Nil,
    pipeline: immutable.Traversable[Stage] = Nil,
    setting: Setting,
    pullMode: Boolean
  ): Props = Props.create(
    classOf[InComingTcpConnectionHandler],
    eventLoopGroup,
    channel,
    commandHandler,
    timeout,
    options,
    pipeline,
    setting,
    boolean2Boolean(pullMode)
  )
}
