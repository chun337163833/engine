package gear.sample.engine.component.io

import java.net.InetSocketAddress

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.util.ByteString
import qgame.akka.extension.netty.transport.WriteCommand.WriteAndFlush
import qgame.akka.extension.netty.transport._

/**
 * Created by kerr.
 */
class ConnectionHandler(
    connector: ActorRef,
    remoteAddress: InetSocketAddress,
    localAddress: InetSocketAddress
) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Active(r, l) =>
      log.debug("connection Active,remote :[{}],local :[{}]", r, l)
      sender() ! WriteAndFlush(ByteString.fromString("pong"))
    case Received(msg) =>
      log.debug("received :[{}]", msg)
      val bs = msg.asInstanceOf[ByteString]
      //sender() ! WriteAndFlush(bs)
      sender() ! WriteAndFlush(bs)
    case InActive(r, l) =>
      log.debug("connection Inactive,remote :[{}],local :[{}]", r, l)
    case c: ConnectionClosed =>
      log.debug("connection closed :[{}]", c)
      context.stop(self)
    case msg =>
      log.warning("unhandled msg :[{}]", msg)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("connection handler pre start for remote address:[{}],local Address :[{}]", remoteAddress, localAddress)
    connector ! Register(self, backPressure = false)
    //    context.system.scheduler.scheduleOnce(10.seconds){
    //      log.debug("schedule an abort command")
    //      //connector ! ConfirmedClose
    //      self ! PoisonPill
    //    }
    super.preStart()
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("connection handler stopped for :[{}],local address :[{}]", remoteAddress, localAddress)
    super.postStop()
  }
}
