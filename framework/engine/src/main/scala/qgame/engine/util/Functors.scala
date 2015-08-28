package qgame.engine.util

/**
 * Created by kerr.
 */
trait ThenHandler[A] {
  def onSuccess(a: A): Unit

  def OnFailure(e: Throwable): Unit
}

trait CompleteHandler[A] {
  def onSuccess(a: A): Unit

  def OnFailure(e: Throwable): Unit
}

trait Transformer[A, B] {
  def onSuccess(a: A): B

  def OnFailure(e: Throwable): Throwable
}