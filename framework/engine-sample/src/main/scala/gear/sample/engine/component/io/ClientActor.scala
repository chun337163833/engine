package gear.sample.engine.component.io

import java.net.InetSocketAddress

import akka.actor.{ Actor, ActorLogging }
import akka.io.IO
import akka.util.ByteString
import qgame.akka.extension.netty.transport.WriteCommand.WriteAndFlush
import qgame.akka.extension.netty.transport._
import qgame.akka.extension.netty.transport.tcp.Tcp

/**
 * Created by kerr.
 */
class ClientActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case Connected(remoteAddress, localAddress) =>
      log.debug("connected to remote :[{}] from :[{}]", remoteAddress, localAddress)
      sender() ! Register(self, backPressure = false)
    case Active(remoteAddress, localAddress) =>
      log.debug("connected and active,starting ping pong.")
      context.become(connected())
      val connector = sender()
      import context.dispatcher

      import scala.concurrent.duration._
      context.system.scheduler.schedule(1.seconds, 1.seconds) {
        connector ! WriteAndFlush(ByteString.fromString("ping"))
      }
    case msg =>
  }

  private def connected(): Receive = {
    case Received(msg) =>
      log.debug("received :[{}]", msg)
      sender() ! WriteAndFlush(msg)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    import context.system
    val manager = IO(Tcp)
    import context.dispatcher

    import scala.concurrent.duration._
    context.system.scheduler.scheduleOnce(3.seconds) {
      manager ! Connect(self, new InetSocketAddress(9000), shared = true)
    }
  }
}
