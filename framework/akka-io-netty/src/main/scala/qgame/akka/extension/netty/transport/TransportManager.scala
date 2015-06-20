package qgame.akka.extension.netty.transport

import java.net.SocketAddress

import akka.actor._
import akka.event.Logging
import io.netty.util.internal.TypeParameterMatcher
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
abstract class TransportManager[A <: SocketAddress](
    setting: Setting,
    supportedAddress: Class[A]
) extends Actor with ActorLogging {
  private val matcher = TypeParameterMatcher.get(supportedAddress)

  override def receive: Receive = {
    case bind: Bind =>
      if (matcher.`match`(bind.localAddress)) {
        handleBind(bind, bind.localAddress.asInstanceOf[A])
      } else {
        sender() ! CommandFailed(bind, info = Option("unsupported address"))
      }
    case connect: Connect =>
      connect.localAddress match {
        case Some(localAddress) =>
          if (matcher.`match`(localAddress)) {
            if (matcher.`match`(connect.remoteAddress)) {
              handleConnect(connect, localAddress.asInstanceOf[Option[A]], connect.remoteAddress.asInstanceOf[A])
            } else {
              sender() ! CommandFailed(connect, info = Option("unsupported remote address"))
            }
          } else {
            sender() ! CommandFailed(connect, info = Option("unsupported local address"))
          }
        case None =>
          if (matcher.`match`(connect.remoteAddress)) {
            handleConnect(connect, None, connect.remoteAddress.asInstanceOf[A])
          } else {
            sender() ! CommandFailed(connect, info = Option("unsupported remote address"))
          }
      }
  }

  protected def handleBind(bind: Bind, bindAddress: A): Unit

  protected def handleConnect(connect: Connect, localAddress: Option[A], remoteAddress: A): Unit

  override def supervisorStrategy: SupervisorStrategy = new OneForOneStrategy()(SupervisorStrategy.stoppingStrategy.decider) {

    override def logFailure(
      context: ActorContext,
      child: ActorRef,
      cause: Throwable,
      decision: SupervisorStrategy.Directive
    ): Unit = {
      val log = Logging(context.system.eventStream, child)
      log.error(cause, "error of child of TcpManager,ref :[{}].", child)
      if (cause.isInstanceOf[DeathPactException]) {
        try {
          log.debug("Closed after handler termination at :[{}]", child)
        } catch {
          case NonFatal(_) =>
        }
      } else super.logFailure(context, child, cause, decision)
    }
  }
}
