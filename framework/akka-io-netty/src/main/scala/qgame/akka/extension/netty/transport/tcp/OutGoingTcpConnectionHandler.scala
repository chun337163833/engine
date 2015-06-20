package qgame.akka.extension.netty.transport.tcp

import akka.actor.{ ActorRef, Props }
import io.netty.channel.socket.SocketChannel
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.concurrent.duration.FiniteDuration

/**
 * Created by kerr.
 */
class OutGoingTcpConnectionHandler(
  channel: SocketChannel,
  commandHandler: ActorRef,
  timeout: FiniteDuration,
  setting: Setting,
  pullMode: Boolean
) extends TcpConnectionHandler(
  channel,
  commandHandler,
  timeout,
  setting,
  pullMode
) {

}

object OutGoingTcpConnectionHandler {
  def props(
    channel: SocketChannel,
    commandHandler: ActorRef,
    timeout: FiniteDuration,
    setting: Setting,
    pullMode: Boolean
  ): Props = Props.create(
    classOf[OutGoingTcpConnectionHandler],
    channel,
    commandHandler,
    timeout,
    setting,
    boolean2Boolean(pullMode)
  )
}
