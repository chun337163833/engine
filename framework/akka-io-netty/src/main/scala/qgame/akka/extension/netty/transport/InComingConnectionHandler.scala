package qgame.akka.extension.netty.transport

import akka.actor.ActorRef
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ Channel, EventLoopGroup }
import qgame.akka.extension.netty.Netty
import qgame.akka.extension.netty.Netty.{ ExecutorGroupStage, InvokerStage, SimpleStage, Stage }
import qgame.akka.extension.netty.transport.InComingConnectionHandler.{ InComingChannelRegisterFailed, InComingChannelRegisterSuccess }
import qgame.akka.extension.netty.transport.tcp.TcpExt.Setting

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
abstract class InComingConnectionHandler[C <: Channel](
  eventLoopGroup: EventLoopGroup,
  channel: C,
  commandHandler: ActorRef,
  timeout: FiniteDuration,
  options: immutable.Traversable[ChannelOption[Channel]] = Nil,
  pipeline: immutable.Traversable[Stage] = Nil,
  setting: Setting,
  pullMode: Boolean
) extends ConnectionHandler[C](
  channel,
  commandHandler,
  timeout,
  setting,
  pullMode
) {
  import Netty.NettyFutureBridge._
  import context.dispatcher

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    registerToEventLoop()
    context.become(waitingRegisterToEventLoop)
  }

  private def registerToEventLoop(): Unit = {
    //1,setup the pipeline
    val p = channel.pipeline()
    pipeline.foreach {
      case SimpleStage(name, channelHandler) =>
        p.addLast(name, channelHandler())
      case InvokerStage(invoker, name, channelHandler) =>
        p.addLast(invoker, name, channelHandler())
      case ExecutorGroupStage(executor, name, channelHandler) =>
        p.addLast(executor, name, channelHandler())
    }

    setupChildChannelOptions(channel)
    channel.config().setAutoRead(false)
    //2,register to the event loop
    eventLoopGroup.register(channel).onComplete {
      case Success(v) =>
        self ! InComingChannelRegisterSuccess(v.asInstanceOf[SocketChannel])
      case Failure(e) =>
        self ! InComingChannelRegisterFailed(channel, e)
    }
  }

  protected def setupChildChannelOptions(childChannel: C): Unit

  private def waitingRegisterToEventLoop: Receive = {
    case InComingChannelRegisterSuccess(ch) =>
      log.debug("incoming socket channel :[{}] registered to event loop.going to notify connected message.", ch)
      notifyConnected(ch.asInstanceOf[C])
    case InComingChannelRegisterFailed(ch, e) =>
      log.error(e, "incoming socket channel :[{}] register to event loop failed.will stop.", ch)
      ch.close().onFailure {
        case _ =>
          ch.unsafe().closeForcibly()
      }
      context.stop(self)
  }
}

private object InComingConnectionHandler {

  sealed trait InComingConnectionHandlerEvent
  case class InComingChannelRegisterSuccess(channel: Channel) extends InComingConnectionHandlerEvent
  case class InComingChannelRegisterFailed(channel: Channel, cause: Throwable) extends InComingConnectionHandlerEvent
}
