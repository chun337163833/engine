package qgame.akka.extension.netty

import java.util.concurrent.CancellationException

import io.netty.channel._
import io.netty.channel.group.{ ChannelGroup, ChannelGroupFuture, ChannelGroupFutureListener }
import io.netty.util.concurrent
import io.netty.util.concurrent.{ GenericFutureListener, EventExecutorGroup }

import scala.concurrent.{ Future, Promise }
import scala.language.implicitConversions

/**
 * Created by kerr.
 */
object Netty {
  sealed trait Stage
  case class SimpleStage(name: String, handler: () => ChannelHandler) extends Stage
  case class InvokerStage(invoker: ChannelHandlerInvoker, name: String, handler: () => ChannelHandler) extends Stage
  case class ExecutorGroupStage(group: EventExecutorGroup, name: String, handler: () => ChannelHandler) extends Stage

  object NettyFutureBridge {
    implicit def proxy(channelFuture: ChannelFuture): Future[Channel] = {
      val promise = Promise[Channel]()
      channelFuture.addListener(new ChannelFutureListener {
        override def operationComplete(f: ChannelFuture): Unit = {
          if (f.isCancelled) {
            promise.failure(new CancellationException("netty channel future is canceled"))
          } else {
            if (f.isSuccess) {
              promise.success(f.channel())
            } else {
              promise.failure(f.cause())
            }
          }
        }
      })
      promise.future
    }

    implicit def proxy(channelGroupFuture: ChannelGroupFuture): Future[ChannelGroup] = {
      val promise = Promise[ChannelGroup]()
      channelGroupFuture.addListener(new ChannelGroupFutureListener {
        override def operationComplete(f: ChannelGroupFuture): Unit =
          if (f.isCancelled) {
            promise.failure(new CancellationException("netty channel group future is canceled"))
          } else {
            if (f.isSuccess) {
              promise.success(f.group())
            } else {
              promise.failure(f.cause())
            }
          }
      })
      promise.future
    }

    implicit def proxy[T](future: io.netty.util.concurrent.Future[T]): Future[T] = {
      val promise = Promise[T]()
      future.addListener(new GenericFutureListener[io.netty.util.concurrent.Future[T]] {
        override def operationComplete(f: concurrent.Future[T]): Unit = {
          if (f.isCancelled) {
            promise.failure(new CancellationException("netty future is canceled"))
          } else {
            if (f.isSuccess) {
              promise.success(f.getNow)
            } else {
              promise.failure(f.cause())
            }
          }
        }
      })
      promise.future
    }

  }
}
