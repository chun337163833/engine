package qgame.akka.extension.netty.stream

import akka.util.ByteString
import io.netty.channel.{ ChannelHandlerContext, SimpleChannelInboundHandler, Channel }
import org.reactivestreams.{ Publisher, Subscription, Subscriber }

/**
 * Created by kerr.
 */
object ChannelStream {
  abstract class AbstractChannelSubscriber[O](channel: Channel) extends Subscriber[O] {
    private var subscription: Subscription = _
    override def onError(t: Throwable): Unit = {
      //stop
    }

    override def onSubscribe(s: Subscription): Unit = {
      subscription = s
    }

    override def onComplete(): Unit = {
      //stop
    }

    protected def requestMore: Int

    protected def transform(t: O): Any = t

    override def onNext(t: O): Unit = {
      channel.writeAndFlush(transform(t))
      //may depends on the type of O
      //for bytes
      if (channel.isWritable) {
        subscription.request(requestMore)
      }
    }
  }

  class BytesChannelSubscriber(channel: Channel) extends AbstractChannelSubscriber[ByteString](channel) {
    //    override protected def requestMore: Int = channel.bytesBeforeUnwritable

    override protected def requestMore: Int = 1

    override protected def transform(t: ByteString): Any = channel.write(channel.alloc().buffer(t.length).writeBytes(t.asByteBuffer))
  }

  class MessageChannelSubscriber[O](channel: Channel) extends AbstractChannelSubscriber[O](channel) {
    override protected def requestMore: Int = 1
  }

  class ChannelPublisher[I](channel: Channel) extends SimpleChannelInboundHandler[I] with Publisher[I] {
    private var demand: Long = 0
    private var subscriber: Subscriber[_ >: I] = _

    override def subscribe(s: Subscriber[_ >: I]): Unit = {
      s.onSubscribe(new Subscription {
        override def cancel(): Unit = channel.close()

        override def request(n: Long): Unit = {
          channel.pipeline().fireUserEventTriggered(Request(n))
        }
      })
      subscriber = s
    }

    override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
      evt match {
        case Request(n) =>
          demand += n
        case _ => super.userEventTriggered(ctx, evt)
      }
    }

    override def channelRead0(ctx: ChannelHandlerContext, msg: I): Unit = {
      subscriber.onNext(msg)
      demand -= 1
    }

    override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
      if (demand > 0) {
        ctx.read()
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      subscriber.onError(cause)
    }

    override def channelInactive(ctx: ChannelHandlerContext): Unit = {
      subscriber.onComplete()
    }
  }

  case class Request(n: Long)
}
