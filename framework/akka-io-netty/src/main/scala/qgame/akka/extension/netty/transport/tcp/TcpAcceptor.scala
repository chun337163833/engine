package qgame.akka.extension.netty.transport.tcp

import akka.actor.{ ActorRef, Props }
import io.netty.channel.epoll.{ EpollEventLoopGroup, EpollServerSocketChannel }
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.{ ServerSocketChannel, SocketChannel }
import io.netty.channel.{ Channel, EventLoopGroup }
import qgame.akka.extension.netty.transport._
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

/**
 * Created by kerr.
 */
final class TcpAcceptor(bind: Bind, bindCommander: ActorRef, setting: Setting)
    extends Acceptor[ServerSocketChannel](bind, bindCommander, setting) {

  override protected def newChannel: ServerSocketChannel = {
    if (setting.native)
      new EpollServerSocketChannel()
    else
      new NioServerSocketChannel()
  }

  override protected def setupChannelOptions(ch: ServerSocketChannel): Unit = {
    setting.Server.options.foreach(_(ch))
    ch match {
      case ech: EpollServerSocketChannel =>
        setting.EpollServer.options.foreach(_(ech))
      case _ =>
    }

    bind.options.foreach(_(ch))
  }

  private lazy val chaildEventLoopGroup: EventLoopGroup = {
    if (bind.shared) {
      if (setting.native) ConnectionHandler.sharedEpollEventLoop else ConnectionHandler.sharedNIOEventLoop
    } else {
      if (setting.native) new EpollEventLoopGroup() else new NioEventLoopGroup()
    }
  }

  override protected def handleIncomingConnection(childChannel: Channel): Unit = {
    context.actorOf(InComingTcpConnectionHandler.props(
      chaildEventLoopGroup,
      childChannel.asInstanceOf[SocketChannel],
      bindHandler,
      registerTimeOut,
      childOptions,
      pipeline,
      setting,
      pullMode
    ))
  }
}

object TcpAcceptor {
  def props(bind: Bind, bindCommander: ActorRef, setting: Setting): Props = Props.create(classOf[TcpAcceptor], bind, bindCommander, setting)
}

