package qgame.engine.libs

import qgame.engine.libs.flows.FutureFlow

import scala.concurrent.Future

/**
 * Created by kerr.
 */
object Flow {
  def fromFuture[T](future: Future[T]): FutureFlow[T] = {
    FutureFlow.source(future)
  }
}
