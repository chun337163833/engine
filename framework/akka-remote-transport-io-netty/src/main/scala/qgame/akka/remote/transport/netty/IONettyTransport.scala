package qgame.akka.remote.transport.netty

import java.net._

import akka.actor.{ Address, ExtendedActorSystem, Props }
import akka.remote.transport.Transport.AssociationEventListener
import akka.remote.transport.{ AssociationHandle, Transport }
import com.typesafe.config.Config
import io.netty.handler.codec.{ LengthFieldBasedFrameDecoder, LengthFieldPrepender }
import qgame.akka.extension.netty.Netty.SimpleStage
import qgame.akka.extension.netty.transport.ConnectionHandler.ConnectionChannelOutBoundHandler
import qgame.akka.extension.netty.transport.Connector
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportManager._
import qgame.engine.config.QGameConfig

import scala.concurrent._

/**
 * Created by kerr.
 */
final class IONettyTransport(system: ExtendedActorSystem, setting: Setting) extends Transport {
  def this(system: ExtendedActorSystem, config: Config) =
    this(system, new Setting(QGameConfig(config)))
  private val manager = system.systemActorOf(Props.create(classOf[TransportManager], setting), "REMOTE-TRANSPORT-IO-NETTY")

  override def listen: Future[(Address, Promise[AssociationEventListener])] = {
    val promise = Promise[(Address, Promise[AssociationEventListener])]()
    manager ! Listen(promise)
    promise.future
  }

  override def associate(remoteAddress: Address): Future[AssociationHandle] = {
    val promise = Promise[AssociationHandle]()
    manager ! Associate(remoteAddress, promise)
    promise.future
  }

  override def shutdown(): Future[Boolean] = {
    val promise = Promise[Boolean]()
    manager ! ShutdownTransport(promise)
    promise.future
  }

  override def maximumPayloadBytes: Int = setting.maximumFrameSize

  override def isResponsibleFor(address: Address): Boolean = true

  override def schemeIdentifier: String = setting.transportProtocal

  //TODO support this?
  override def managementCommand(cmd: Any): Future[Boolean] = super.managementCommand(cmd)
}

object IONettyTransport {
  class Setting(config: QGameConfig) {
    private val validProtocal = Set("tcp")
    val transportProtocal = config.getString("transport-protocal").getOrElse(
      throw new IllegalArgumentException("must setup transport-protocal in config section 'akka.remote.io-netty'.")
    )
    require(validProtocal(transportProtocal), s"Setting 'transport protocal' must be one of $validProtocal,but you provide :[$transportProtocal]")

    val transportType: TransportType = transportProtocal match {
      case "tcp" => TransportType.Tcp
      case unsupported => throw new IllegalArgumentException(s"$unsupported is a currently unsupported transport protocal")
    }
    val maximumFrameSize = config.getBytes("maximum-frame-size").getOrElse(
      throw new IllegalArgumentException(s"must set up maximum-frame-size in config section 'akka.remote.io-netty.$transportProtocal'.")
    ).toInt
    require(maximumFrameSize > 32000, s"Setting 'maximum-frame-size' must be at least 32000 bytes,but you provide $maximumFrameSize")

    //TODO fix this up for the udp transport
    object Tcp {
      val frameLengthFieldLength = config.getInt("frame-length-field-length").getOrElse(
        throw new IllegalArgumentException(s"must setup frame-length-field-length in config section 'akka.remote.io-netty.$transportProtocal'.")
      )
      val stage = List(
        SimpleStage("frameEncoder", () => new LengthFieldPrepender(frameLengthFieldLength, false)),
        SimpleStage("frameDecoder", () => new LengthFieldBasedFrameDecoder(
          maximumFrameSize,
          0,
          frameLengthFieldLength,
          0,
          frameLengthFieldLength,
          true
        )),
        SimpleStage("outBoundEncoder", () => new ConnectionChannelOutBoundHandler)
      )
    }

    val port = config.getInt("port").getOrElse(
      throw new IllegalArgumentException(s"must set up port in config section 'akka.remote.io-netty.$transportProtocal'.")
    )

    val hostName = config.getString("hostname").map {
      case "" => InetAddress.getLocalHost.getHostAddress
      case hostname => hostname
    }.getOrElse(throw new IllegalArgumentException(s"must set up hostname in config section 'akka.remote.io-netty.$transportProtocal'."))

    val hostAddress = try {
      InetAddress.getByName(hostName)
    } catch {
      case _: UnknownHostException =>
        throw new IllegalArgumentException("unknown host address,please check")
    }
    require(hostAddress.isInstanceOf[Inet4Address] || hostAddress.isInstanceOf[Inet6Address], "HostAddress must be ipv4 or ipv6 address")

  }

  def addressToSocketAddress(addr: Address)(implicit ex: ExecutionContext): Future[InetSocketAddress] = addr match {
    case Address(_, _, Some(h), Some(p)) ⇒
      import qgame.akka.extension.netty.Netty.NettyFutureBridge._
      Connector.resolve(h, p)
    case _ ⇒ Future.failed(new IllegalArgumentException(s"Address [$addr] does not contain host or port information."))
  }

}
