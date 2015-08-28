package qgame.akka.extension.netty.transport.tcp

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.IO.Extension
import io.netty.channel.epoll.EpollMode
import io.netty.util.NetUtil
import qgame.akka.extension.netty.transport.SocketChannelOption.TrafficClass
import qgame.akka.extension.netty.transport._
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.engine.config.QGameConfig

/**
 * Created by kerr.
 */
object Tcp extends ExtensionKey[TcpExt] {
}

class TcpExt(system: ExtendedActorSystem) extends Extension {
  //return the manager of netty io
  val setting = new Setting(QGameConfig(system.settings.config).getConfig("akka.extension.netty.tcp").getOrElse(
    throw new IllegalArgumentException("must setup config section of akka.extension.netty.tcp")
  ))
  val manager: ActorRef = system.systemActorOf(Props.create(classOf[TcpManager], setting), "IO-Netty-TCP")
  def getManager = manager
}

private[netty] object TcpExt {

  class Setting(config: QGameConfig) {
    val preferNative = config.getBoolean("preferNative").getOrElse(true)
    //val native = if (Platform.isLinux && preferNative) true else false
    val native = false

    object Server {
      val autoAccept = ServerChannelOptions.autoAccept(config.getBoolean("server.auto-accept").getOrElse(true))
      val maxConnPerAccept = ServerChannelOptions.maxConnPerAccept(config.getInt("server.max-conn-per-accept").getOrElse(16))

      val backlog = ServerSocketChannelOptions.backlog(config.getInt("server.backlog").getOrElse(NetUtil.SOMAXCONN))
      val reuseAddress = ServerSocketChannelOptions.reuseAddress(config.getBoolean("server.reuse-address").getOrElse(true))
      lazy val options = List(backlog, reuseAddress, autoAccept, maxConnPerAccept)
    }

    object Connection {
      val maxMessagePerRead = ChannelOptions.maxMessagePerRead(config.getInt("connection.max-message-per-read").getOrElse(1))
      val writeSpinCount = ChannelOptions.writeSpinCount(config.getInt("connection.write-spin-count").getOrElse(16))
      val writeBufferHighWaterMark = ChannelOptions.writeBufferHighWaterMark(config.getBytes("connection.write-buffer-high-water-mark").map(_.toInt).getOrElse(64 * 1024))
      val writeBufferLowWaterMark = ChannelOptions.writeBufferLowWaterMark(config.getBytes("connection.write-buffer-low-water-mark").map(_.toInt).getOrElse(64 * 1024))
      val autoRead = ChannelOptions.autoRead(config.getBoolean("connection.auto-read").getOrElse(true))

      val allowHalfClose = SocketChannelOptions.allowHalfClose(config.getBoolean("connection.allow-half-close").getOrElse(true))
      val keepAlive = SocketChannelOptions.keepAlive(config.getBoolean("connection.keep-alive").getOrElse(true))
      val tcpNoDelay = SocketChannelOptions.tcpNoDelay(config.getBoolean("connection.tcp-no-delay").getOrElse(true))
      val trafficClass = SocketChannelOptions.trafficClass(config.getString("connection.traffic-class").collect {
        case "LOWCOST" => TrafficClass.IPTOS_LOWCOST
        case "RELIABILITY" => TrafficClass.IPTOS_RELIABILITY
        case "THROUGHPUT" => TrafficClass.IPTOS_THROUGHPUT
        case "LOWDELAY" => TrafficClass.IPTOS_LOWDELAY
      }.getOrElse(TrafficClass.IPTOS_RELIABILITY))
      val performancePreferences = {
        val Seq(connectionTime, latency, bandwidth) = config.getIntSeq("connection.performance-preferences")
          .map(_.map(_.intValue())).getOrElse(Seq(0, 1, 2))
        SocketChannelOptions.performancePreferences(connectionTime, latency, bandwidth)
      }
      val reuseAddress = SocketChannelOptions.reuseAddress(config.getBoolean("connection.reuse-address").getOrElse(true))
      val linger = SocketChannelOptions.linger(config.getInt("connection-linger").getOrElse(5))
      lazy val options = List(maxMessagePerRead, writeSpinCount, writeBufferHighWaterMark, writeBufferLowWaterMark,
        allowHalfClose, autoRead, keepAlive, tcpNoDelay, trafficClass, performancePreferences, reuseAddress, linger)
    }

    object EpollServer {
      val reusePort = EpollServerSocketChannelOptions.reusePort(config.getBoolean("epoll-server.reuse-port").getOrElse(false))
      lazy val options = List(reusePort)
    }

    //http://tldp.org/HOWTO/TCP-Keepalive-HOWTO/usingkeepalive.html
    object EpollConnection {
      val tcpCork = EpollSocketChannelOptions.tcpCork(config.getBoolean("epoll-connection.tcp-cork").getOrElse(false))
      val tcpKeepIdle = EpollSocketChannelOptions.tcpKeepIdle(config.getDuration("epoll-connection.tcp-keep-idle", TimeUnit.SECONDS).map(_.toInt).getOrElse(600))
      val tcpKeepIntvl = EpollSocketChannelOptions.tcpKeepIntvl(config.getDuration("epoll-connection.tcp-keep-intvl", TimeUnit.SECONDS).map(_.toInt).getOrElse(60))
      val tcpKeepCnt = EpollSocketChannelOptions.tcpKeepCnt(config.getInt("epoll-connection.tcp-keep-cnt").getOrElse(20))
      val mode = {
        val mode = config.getString("epoll-connection.mode").collect {
          case "ET" => EpollMode.EDGE_TRIGGERED
          case "LT" => EpollMode.LEVEL_TRIGGERED
        }.getOrElse(EpollMode.EDGE_TRIGGERED)
        EpollSocketChannelOptions.mode(mode)
      }

      lazy val options = List(tcpCork, tcpKeepIdle, tcpKeepIntvl, tcpKeepCnt, mode)
    }
  }

}