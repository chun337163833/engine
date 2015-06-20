package qgame.akka.remote.transport.netty

import akka.actor._
import akka.remote.transport.AssociationHandle
import akka.remote.transport.Transport.AssociationEventListener
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportManager._

import scala.concurrent.{ Future, Promise }
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
private[netty] final class TransportManager(setting: Setting) extends Actor with ActorLogging {
  private val listener = context.actorOf(Props.create(classOf[TransportListener], setting), "listener")
  private val associator = context.actorOf(Props.create(classOf[TransportAssociator], setting), s"associator")

  override def receive: Receive = {
    case l: Listen =>
      log.debug("transport manager start listener at :[{}].")
      listener ! l
    case a @ Associate(remoteAddress, _) =>
      log.debug("transport manager start associate to remote address :[{}]", remoteAddress)
      associator ! a
    case ShutdownTransport(promise) =>
      log.debug("transport manager is going to shutdown the transport.")
      val shutdownListenerPromise = Promise[Boolean]()
      val shutdownAssociatorPromise = Promise[Boolean]()
      listener ! ShutdownListener(shutdownListenerPromise)
      associator ! ShutdownAssociator(shutdownAssociatorPromise)
      import context.dispatcher
      val shutdownTransportFuture = Future.reduce(List(shutdownListenerPromise.future, shutdownAssociatorPromise.future)) {
        case (l, a) => l && a
      }
      promise.completeWith(shutdownTransportFuture)
      shutdownTransportFuture.onComplete {
        case Success(v) =>
          self ! PoisonPill
        case Failure(e) =>
          self ! PoisonPill
      }
  }

}

private[netty] object TransportManager {
  sealed trait TransportType

  object TransportType {
    case object Tcp extends TransportType
  }

  sealed trait Command

  case class Listen(promise: Promise[(Address, Promise[AssociationEventListener])]) extends Command

  case class Associate(remote: Address, promise: Promise[AssociationHandle]) extends Command

  case class ShutdownTransport(promise: Promise[Boolean]) extends Command

  case class ShutdownListener(promise: Promise[Boolean]) extends Command

  case class ShutdownAssociator(promise: Promise[Boolean]) extends Command
}