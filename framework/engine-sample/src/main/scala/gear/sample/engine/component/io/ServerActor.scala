package gear.sample.engine.component.io

import java.net.InetSocketAddress

import akka.actor.{ Actor, ActorLogging, Props }
import akka.io.IO
import qgame.akka.extension.netty.transport.tcp.Tcp
import qgame.akka.extension.netty.transport.{ Bind, Bound, Connected }

/**
 * Created by kerr.
 */
class ServerActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case Bound(address) =>
      log.debug("bound :[{}]", address)
    case Connected(remoteAddress, localAddress) =>
      val connector = sender()
      log.debug("connected,remote :[{}],local :[{}],connector :[{}]", remoteAddress, localAddress, connector)
      context.actorOf(Props.create(classOf[ConnectionHandler], connector, remoteAddress, localAddress))

  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    import context.system
    val manager = IO(Tcp)
    manager ! Bind(self, new InetSocketAddress(9000))
    super.preStart()
  }
}
