package qgame.akka.extension.netty.transport

import io.netty.channel.ServerChannel
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.socket.ServerSocketChannel
import qgame.akka.extension.netty.transport.EpollServerChannelOption.ReusePort
import qgame.akka.extension.netty.transport.ServerChannelOption.{ AutoAccept, MaxConnPerAccept }
import qgame.akka.extension.netty.transport.ServerSocketChannelOption.{ Backlog, ReuseAddress }

/**
 * Created by kerr.
 */
sealed trait ServerChannelOption[C <: ServerChannel] extends (C => Unit)

object ServerChannelOption {
  private[netty] case class AutoAccept(value: Boolean) extends ServerChannelOption[ServerChannel] {
    override def apply(channel: ServerChannel): Unit = channel.config().setAutoRead(value)
  }

  case class MaxConnPerAccept(num: Int) extends ServerChannelOption[ServerChannel] {
    require(num >= 1)
    override def apply(channel: ServerChannel): Unit = channel.config().setMaxMessagesPerRead(num)
  }
}

object ServerChannelOptions {
  private[netty] def autoAccept(value: Boolean): ServerChannelOption[ServerChannel] = AutoAccept(value)

  def maxConnPerAccept(num: Int): ServerChannelOption[ServerChannel] = MaxConnPerAccept(num)
}

sealed trait ServerSocketChannelOption extends ServerChannelOption[ServerSocketChannel]

object ServerSocketChannelOption {
  case class Backlog(size: Int) extends ServerSocketChannelOption {
    override def apply(channel: ServerSocketChannel): Unit = channel.config().setBacklog(size)
  }

  case class ReuseAddress(value: Boolean) extends ServerSocketChannelOption {
    override def apply(channel: ServerSocketChannel): Unit = channel.config().setReuseAddress(value)
  }
}

object ServerSocketChannelOptions {
  def backlog(size: Int): ServerSocketChannelOption = Backlog(size)

  def reuseAddress(value: Boolean): ServerSocketChannelOption = ReuseAddress(value)
}

sealed trait EpollServerChannelOption extends ServerChannelOption[EpollServerSocketChannel]

object EpollServerChannelOption {
  case class ReusePort(value: Boolean) extends EpollServerChannelOption {
    override def apply(channel: EpollServerSocketChannel): Unit = channel.config().setReusePort(value)
  }
}

object EpollServerSocketChannelOptions {
  def reusePort(value: Boolean): EpollServerChannelOption = ReusePort(value)
}
