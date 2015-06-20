package qgame.akka.extension.netty.transport.tcp

import akka.actor.ActorRef
import io.netty.channel.socket.SocketChannel
import qgame.akka.extension.netty.Netty
import qgame.akka.extension.netty.transport.ConnectionHandler
import qgame.akka.extension.netty.transport.ConnectionHandler.ConnectionHandlerEvent.{ ConnectionOutputShutdownFailed, ConnectionOutputShutdownSuccess }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
class TcpConnectionHandler(
  channel: SocketChannel,
  commandHandler: ActorRef,
  timeout: FiniteDuration,
  setting: Setting,
  pullMode: Boolean = false
) extends ConnectionHandler[SocketChannel](
  channel,
  commandHandler,
  timeout,
  setting,
  pullMode
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
}
