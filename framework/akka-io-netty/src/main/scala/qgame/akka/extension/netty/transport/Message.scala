package qgame.akka.extension.netty.transport

import java.io.File
import java.net.SocketAddress
import java.util.concurrent.TimeUnit

import akka.actor.{ ActorRef, NoSerializationVerificationNeeded }
import io.netty.channel.{ Channel, ServerChannel }
import qgame.akka.extension.netty.Netty.Stage

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

/**
 * Created by kerr.
 */
sealed trait Message extends NoSerializationVerificationNeeded

trait HasFailureMessage {
  def failureMessage: Any
}

trait Command extends Message

//Bind -> Bound
case class Bind(
  handler: ActorRef,
  localAddress: SocketAddress,
  registerTimeOut: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS), //TODO make this configurable
  options: immutable.Traversable[ServerChannelOption[ServerChannel]] = Nil,
  childOptions: immutable.Traversable[ChannelOption[Channel]] = Nil,
  pipeline: immutable.Traversable[Stage] = Nil,
  pullMode: Boolean = false,
  shared: Boolean = false
) extends Command

case object Unbind extends Command

case object ResumeWriting extends Command

case object SuspendReading extends Command

case object ResumeReading extends Command

case object SuspendAccepting extends Command

case class AutoReading(on: Boolean) extends Command

case class AutoAccepting(on: Boolean) extends Command

case class ResumeAccepting(batchSize: Int) extends Command

//Connected -> Active
case class Register(handler: ActorRef, backPressure: Boolean = true, autoRelease: Boolean = true, autoFlush: Boolean = true) extends Command

sealed trait WriteCommand extends Command

object WriteCommand {
  case class Write(msg: Any) extends WriteCommand

  case class WriteAndFlush(msg: Any) extends WriteCommand

  case object Flush extends WriteCommand {
    def getInstance: Flush.type = Flush
  }

  case class WriteFile(file: File, position: Long, count: Long) extends WriteCommand
}

sealed trait CloseCommand extends Command

object CloseCommand {
  //flush all pending
  case object Close extends CloseCommand {
    def getInstance = Close
  }

  //flush all pending & shutdown output
  case object ConfirmedClose extends CloseCommand {
    def getInstance = ConfirmedClose
  }

  //close immediately
  case object Abort extends CloseCommand {
    def getInstance = Abort
  }

}

case class Connect(
  handler: ActorRef,
  remoteAddress: SocketAddress,
  localAddress: Option[SocketAddress] = None,
  registerTimeOut: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS),
  options: immutable.Traversable[ChannelOption[Channel]] = Nil,
  pipeline: immutable.Traversable[Stage] = Nil,
  pullMode: Boolean = false,
  shared: Boolean = false
) extends Command

//////////////////////////////////////

trait Event extends Message

case class CommandFailed(command: Command, cause: Option[Throwable] = None, info: Option[String] = None) extends Event

//Bound -> Connected
case class Bound(localAddress: SocketAddress) extends Event

case class Connected(remoteAddress: SocketAddress, localAddress: SocketAddress) extends Event

case class Active(remoteAddress: SocketAddress, localAddress: SocketAddress) extends Event

case class InActive(remoteAddress: SocketAddress, localAddress: SocketAddress) extends Event

sealed trait ConnectionClosed extends Event
object ConnectionClosed {
  case object Closed extends ConnectionClosed {
    def getInstance = Closed
  }

  case object ConfirmedClosed extends ConnectionClosed {
    def getInstance = ConfirmedClosed
  }

  case object Aborted extends ConnectionClosed {
    def getInstance = Aborted
  }

  case object PeerClosed extends ConnectionClosed {
    def getInstance = PeerClosed
  }

  case class ErrorClosed(cause: Throwable) extends ConnectionClosed
}

case class Received(any: Any) extends Event