package qgame.akka.extension.netty.transport.tcp

import akka.actor._
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.akka.extension.netty.transport.{ Connect, Connector }

/**
 * Created by kerr.
 */

final class TcpConnector(
  connect: Connect,
  connectCommander: ActorRef,
  setting: Setting
) extends Connector[SocketChannel](
  connect, connectCommander, setting
) {
  override protected def newChannel: SocketChannel = {
    if (setting.native) {
      new EpollSocketChannel()
    } else {
      new NioSocketChannel()
    }
  }

  override protected def setupChannelOptions(ch: SocketChannel): Unit = {
    setting.Connection.options.foreach(_(ch))
    ch match {
      case ech: EpollSocketChannel =>
        setting.EpollConnection.options.foreach(_(ech))
      case _ =>
    }
    options.foreach(_(ch))
  }

  override protected def handleOutGoingConnection(channel: SocketChannel): Unit = {
    val connectionHandler = context.actorOf(OutGoingTcpConnectionHandler.props(
      channel, connectCommander, registerTimeOut, setting, pullMode
    ))
    context.watch(connectionHandler)
  }

}

object TcpConnector {
  def props(
    connect: Connect,
    connectCommander: ActorRef,
    setting: Setting
  ): Props =
    Props.create(classOf[TcpConnector], connect, connectCommander, setting)

}
