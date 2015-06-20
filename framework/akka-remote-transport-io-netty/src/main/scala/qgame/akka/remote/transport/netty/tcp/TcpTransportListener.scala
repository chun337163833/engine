package qgame.akka.remote.transport.netty.tcp

import java.net.InetSocketAddress

import akka.actor._
import akka.io.IO
import akka.remote.transport.Transport.AssociationEventListener
import qgame.akka.extension.netty.transport._
import qgame.akka.extension.netty.transport.tcp.Tcp
import qgame.akka.remote.transport.netty.IONettyTransport.Setting
import qgame.akka.remote.transport.netty.TransportListener.{ AssociationEventListenerRegisterFailed, AssociationEventListenerRegisterSuccess }
import qgame.akka.remote.transport.netty.TransportManager.{ ShutdownTransport, Listen }

import scala.concurrent.Promise
import scala.util.{ Failure, Success, Try }

/**
 * Created by kerr.
 */
private[netty] final class TcpTransportListener(setting: Setting) extends Actor with Stash with ActorLogging {
  private val localAddress = new InetSocketAddress(setting.hostAddress, setting.port)
  import context.{ dispatcher, system }
  private val manager = IO(Tcp)

  override def receive: Receive = {
    case Listen(promise) =>
      log.debug("tcp transport listener start binding at :[{}]", localAddress)
      manager ! Bind(self, localAddress, pipeline = setting.Tcp.stage, shared = true)
      context.become(waitingBound(promise))
  }

  private def waitingBound(promise: Promise[(Address, Promise[AssociationEventListener])]): Receive = {
    case Bound(boundAddress) =>
      log.debug("tcp transport listener bound at :[{}]", boundAddress)
      //complete the promise
      val listenerPromise = Promise[AssociationEventListener]()
      //TODO change to akka 2.4's bind address :)
      val boundAddr = boundAddress.asInstanceOf[InetSocketAddress]
      promise.tryComplete(Try((Address("tcp", context.system.name, boundAddr.getHostString, boundAddr.getPort), listenerPromise)))
      val acceptor = sender()
      listenerPromise.future.onComplete {
        case Success(listener) =>
          self ! AssociationEventListenerRegisterSuccess(listener)
        case Failure(e) =>
          self ! AssociationEventListenerRegisterFailed(e)
      }
      context.become(waitingAssociationEventListenerRegistered(acceptor))
  }

  private def waitingAssociationEventListenerRegistered(acceptor: ActorRef): Receive = {
    case AssociationEventListenerRegisterSuccess(listener) =>
      //resume accept
      //      acceptor ! ResumeAccepting(1)
      unstashAll()
      context.become(accepting(listener, acceptor))
    case AssociationEventListenerRegisterFailed(e) =>
      //shutdown
      log.error(e, "association event listener register failed,shutdown.")
      acceptor ! Unbind
      context.stop(self)
    case Connected(remoteAddr, localAddr) =>
      log.warning("remote association connected,remote address :[{}],local address :[{}],while waiting association event listener registered,stash it.", remoteAddr, localAddr)
      stash()
  }

  private def accepting(listener: AssociationEventListener, acceptor: ActorRef): Receive = {
    case Connected(remoteAddr, localAddr) =>
      log.debug("association in bound,local address :[{}],remote address :[{}]", localAddr, remoteAddr)
      context.actorOf(TcpTransportAssociationInboundHandler.props(setting, sender(), listener))
      acceptor ! ResumeAccepting(1)
    case ShutdownTransport(promise) =>
      log.debug("shutdown transport ,the tcp transport listener side.")
      acceptor ! Unbind
      promise.trySuccess(true)
      context.stop(self)
  }

}