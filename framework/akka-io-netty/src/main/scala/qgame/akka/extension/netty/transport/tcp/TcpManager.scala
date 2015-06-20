package qgame.akka.extension.netty.transport.tcp

import java.net.InetSocketAddress

import akka.actor._
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting
import qgame.akka.extension.netty.transport.{ Bind, CommandFailed, Connect, TransportManager }

/**
 * Created by kerr.
 */
final class TcpManager(setting: Setting) extends TransportManager[InetSocketAddress](
  setting, classOf[InetSocketAddress]
) {

  override protected def handleBind(bind: Bind, bindAddress: InetSocketAddress): Unit = {
    bind match {
      case Bind(handler, address, _, _, _, _, pullMode, shared) =>
        log.debug("going to start tcp listener for handler:[{}],at address :[{}],prefer native :[{}],pull mode :[{}]", handler, address, setting.preferNative, pullMode)
        val bindCommander = sender()
        val actorName = s"listening-${bindAddress.getHostString}:${bindAddress.getPort}"
        context.child(actorName) match {
          case Some(ref) =>
            log.error("there is an acceptor :[{}] for address :[{}] already.", ref, address)
            bindCommander ! CommandFailed(bind, info = Option("there is an acceptor for that address already."))
          case None =>
            context.actorOf(TcpAcceptor.props(bind, bindCommander, setting), actorName)
        }
    }
  }

  override protected def handleConnect(connect: Connect, localAddress: Option[InetSocketAddress], remoteAddress: InetSocketAddress): Unit = {
    connect match {
      case Connect(handler, _, _, _, _, _, pullMode, shared) =>
        log.debug("going to start tcp connector for handler :[{}] ,with connection from :[{}] to :[{}],pull mode :[{}].", handler, localAddress, remoteAddress, pullMode)
        val connectCommander = sender()
        val actorName = s"connect-${localAddress.map(address => s"${address.getHostString}:${address.getPort}")}-${remoteAddress.getHostString}:${remoteAddress.getPort}"
        context.child(actorName) match {
          case Some(ref) =>
            localAddress match {
              case Some(addr) =>
                log.error("there is an connector :[{}] from :[{}] to :[{}] already.", ref, addr, remoteAddress)
                connectCommander ! CommandFailed(connect, info = Option("there is an connector from that address to the target address already."))
              case None =>
                log.warning("there is an connector to address :[{}] already at :[{}],but will start an new connect now from an random local address.", remoteAddress, ref)
                //TODO random name here
                context.actorOf(TcpConnector.props(connect, connectCommander, setting))
            }
          case None =>
            context.actorOf(TcpConnector.props(connect, connectCommander, setting), actorName)
        }
    }
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    log.debug("netty TcpManager preStart.")
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("netty TcpManager postStop.")
    super.postStop()
  }

}
