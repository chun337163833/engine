package qgame.akka.extension.netty.transport

import io.netty.channel.{ Channel, EventLoopGroup }

/**
 * Created by kerr.
 */
trait ChannelBootStrap[T <: Channel] {
  protected def eventLoopGroup: EventLoopGroup
  protected def newChannel: T
  protected def init(): Unit
  protected def register(): Unit
  protected def shutdown(): Unit
}
