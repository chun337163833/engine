package qgame.engine.libs.flows

import qgame.engine.libs.Functions.Function1

import scala.concurrent.{ ExecutionContext, Future, Promise }

/**
 * Created by kerr.
 */
case class FutureFlow[T](source: Future[T]) extends FlowLike {

  override type IN = Future[T]

  override def output: OUT = source

  override type OUT = Future[T]

  def map[B](mapper: Function1[T, B]): FutureFlow[B] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    FutureFlowHelper.map(mapper)
  }

  def map[B](mapper: Function1[T, B])(implicit ex: ExecutionContext): FutureFlow[B] = {
    FutureFlowHelper.map(mapper)
  }

  def mapAsync[B](mapper: Function1[T, Future[B]]): FutureFlow[B] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    FutureFlowHelper.mapAsync(mapper)
  }

  def attachTo(promise: Promise[T]): Unit = {
    FutureFlowHelper.attachTo(promise)
  }

  object FutureFlowHelper {
    def map[B](mapper: Function1[T, B])(implicit ex: ExecutionContext): FutureFlow[B] = {
      FutureFlow {
        source.map(mapper.apply)
      }
    }

    def mapAsync[B](mapper: Function1[T, Future[B]])(implicit ex: ExecutionContext): FutureFlow[B] = {
      FutureFlow {
        source.flatMap(mapper.apply)
      }
    }

    def attachTo(promise: Promise[T]): Unit = {
      promise.tryCompleteWith(output)
    }
  }

}

object FutureFlow {
  def source[T](future: Future[T]): FutureFlow[T] = {
    FutureFlow(future)
  }
}
