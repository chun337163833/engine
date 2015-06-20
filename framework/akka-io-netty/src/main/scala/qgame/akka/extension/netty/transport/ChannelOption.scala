package qgame.akka.extension.netty.transport

import io.netty.channel.Channel
import io.netty.channel.epoll.{ EpollMode, EpollSocketChannel }
import io.netty.channel.socket.SocketChannel
import qgame.akka.extension.netty.transport.ChannelOption._
import qgame.akka.extension.netty.transport.EpollChannelOption._
import qgame.akka.extension.netty.transport.SocketChannelOption._

/**
 * Created by kerr.
 */

sealed trait ChannelOption[C <: Channel] extends (C => Unit)

object ChannelOption {
  case class MaxMessagePerRead(num: Int) extends ChannelOption[Channel] {
    override def apply(channel: Channel): Unit = channel.config().setMaxMessagesPerRead(num)
  }

  case class WriteSpinCount(num: Int) extends ChannelOption[Channel] {
    override def apply(channel: Channel): Unit = channel.config().setWriteSpinCount(num)
  }

  case class WriteBufferHighWaterMark(size: Int) extends ChannelOption[Channel] {
    override def apply(channel: Channel): Unit = channel.config().setWriteBufferHighWaterMark(size)
  }

  case class WriteBufferLowWaterMark(size: Int) extends ChannelOption[Channel] {
    override def apply(channel: Channel): Unit = channel.config().setWriteBufferHighWaterMark(size)
  }

  private[netty] case class AutoRead(value: Boolean) extends ChannelOption[Channel] {
    override def apply(channel: Channel): Unit = channel.config().setAutoRead(value)
  }
}

object ChannelOptions {
  def maxMessagePerRead(num: Int): ChannelOption[Channel] = MaxMessagePerRead(num)

  def writeSpinCount(num: Int): ChannelOption[Channel] = WriteSpinCount(num)

  def writeBufferHighWaterMark(size: Int): ChannelOption[Channel] = WriteBufferHighWaterMark(size)

  def writeBufferLowWaterMark(size: Int): ChannelOption[Channel] = WriteBufferLowWaterMark(size)

  private[netty] def autoRead(value: Boolean): ChannelOption[Channel] = AutoRead(value)

}

sealed trait SocketChannelOption extends ChannelOption[SocketChannel]

object SocketChannelOption {
  case class AllowHalfClose(value: Boolean) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setAllowHalfClosure(value)
  }

  case class KeepAlive(value: Boolean) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setKeepAlive(value)
  }

  case class SendBufferSize(size: Int) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setSendBufferSize(size)
  }

  case class ReceiveBufferSize(size: Int) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setReceiveBufferSize(size)
  }

  case class TcpNoDelay(value: Boolean) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setTcpNoDelay(value)
  }

  case class ReuseAddress(value: Boolean) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setReuseAddress(value)
  }

  case class Linger(value: Int) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setSoLinger(value)
  }

  /**
   * <UL>
   * <LI><CODE>IPTOS_LOWCOST (0x02)</CODE></LI>
   * <LI><CODE>IPTOS_RELIABILITY (0x04)</CODE></LI>
   * <LI><CODE>IPTOS_THROUGHPUT (0x08)</CODE></LI>
   * <LI><CODE>IPTOS_LOWDELAY (0x10)</CODE></LI>
   * </UL>
   */
  case class TrafficClass(tc: Int) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setTrafficClass(tc)
  }

  object TrafficClass {
    val IPTOS_LOWCOST = 0x02
    val IPTOS_RELIABILITY = 0x04
    val IPTOS_THROUGHPUT = 0x08
    val IPTOS_LOWDELAY = 0x10
  }

  case class PerformancePreference(connectionTime: Int, latency: Int, bandwidth: Int) extends SocketChannelOption {
    override def apply(channel: SocketChannel): Unit = channel.config().setPerformancePreferences(connectionTime, latency, bandwidth)
  }

}

object SocketChannelOptions {
  def allowHalfClose(value: Boolean): SocketChannelOption = AllowHalfClose(value)

  def keepAlive(value: Boolean): SocketChannelOption = KeepAlive(value)

  def sendBufferSize(size: Int): SocketChannelOption = SendBufferSize(size)

  def receiveBufferSize(size: Int): SocketChannelOption = ReceiveBufferSize(size)

  def tcpNoDelay(value: Boolean): SocketChannelOption = TcpNoDelay(value)

  def reuseAddress(value: Boolean): SocketChannelOption = ReuseAddress(value)

  def linger(value: Int): SocketChannelOption = Linger(value)

  def trafficClass(tc: Int): SocketChannelOption = TrafficClass(tc)

  def performancePreferences(connectionTime: Int, latency: Int, bandwidth: Int): SocketChannelOption = PerformancePreference(connectionTime, latency, bandwidth)

}

sealed trait EpollChannelOption extends ChannelOption[EpollSocketChannel]

object EpollChannelOption {
  case class TcpCork(value: Boolean) extends EpollChannelOption {
    override def apply(channel: EpollSocketChannel): Unit = channel.config().setTcpCork(value)
  }

  case class TcpKeepIdle(seconds: Int) extends EpollChannelOption {
    override def apply(channel: EpollSocketChannel): Unit = channel.config().setTcpKeepIdle(seconds)
  }

  case class TcpKeepIntvl(seconds: Int) extends EpollChannelOption {
    override def apply(channel: EpollSocketChannel): Unit = channel.config().setTcpKeepIntvl(seconds)
  }

  case class TcpKeepCnt(probes: Int) extends EpollChannelOption {
    override def apply(channel: EpollSocketChannel): Unit = channel.config().setTcpKeepCntl(probes)
  }

  case class Mode(mode: EpollMode) extends EpollChannelOption {
    override def apply(channel: EpollSocketChannel): Unit = channel.config().setEpollMode(mode)
  }

}

object EpollSocketChannelOptions {
  //  val default: List[EpollChannelOption] = List(
  //    tcpCork(value = false),
  //    TcpKeepIdle(600),
  //    tcpKeepIntvl(60),
  //    tcpKeepCnt(20),
  //    mode(EpollMode.EDGE_TRIGGERED)
  //  )

  def tcpCork(value: Boolean): EpollChannelOption = TcpCork(value)

  def tcpKeepIdle(seconds: Int): EpollChannelOption = TcpKeepIdle(seconds)

  def tcpKeepIntvl(seconds: Int): EpollChannelOption = TcpKeepIntvl(seconds)

  def tcpKeepCnt(probes: Int): EpollChannelOption = TcpKeepCnt(probes)

  def mode(mode: EpollMode): EpollChannelOption = Mode(mode)

}

