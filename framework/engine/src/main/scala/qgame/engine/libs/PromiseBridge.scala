package qgame.engine.libs

import java.util.concurrent.Callable
import java.util.function.Supplier

import scala.concurrent.{ Future, Promise }
import scala.util.Try

/**
 * Created by kerr.
 */
object PromiseBridge {
  def apply[T](promise: Promise[T]): PromiseBridge[T] = new PromiseBridge[T](promise)
}

class PromiseBridge[T](val promise: Promise[T]) extends AnyVal {
  def complete(trier: Supplier[T]): PromiseBridge[T] = {
    PromiseBridge {
      promise.complete {
        Try(trier.get())
      }
    }
  }

  def completeWith(other: Future[T]): PromiseBridge[T] = {
    PromiseBridge {
      promise.completeWith(other)
    }
  }

  def failure(cause: Throwable): PromiseBridge[T] = {
    PromiseBridge {
      promise.failure(cause)
    }
  }

  def future: Future[T] = {
    promise.future
  }

  def isComplete: Boolean = {
    promise.isCompleted
  }

  def success(value: T): PromiseBridge[T] = {
    PromiseBridge {
      promise.success(value)
    }
  }

  def tryComplete(trier: Supplier[T]): Boolean = {
    promise.tryComplete(Try(trier.get()))
  }

  def tryCompleteWith(other: Future[T]): PromiseBridge[T] = {
    PromiseBridge {
      promise.completeWith(other)
    }
  }

  def tryFailure(cause: Throwable): Boolean = {
    promise.tryFailure(cause)
  }

  def trySuccess(value: T): Boolean = {
    promise.trySuccess(value)
  }

  //
  //
  //
  // below is extra

  def completeWithBridge(futureBridge: FutureBridge[T]): PromiseBridge[T] = {
    promise.completeWith(futureBridge.unWrap)
    this
  }

  def success(callable: Callable[T]): PromiseBridge[T] = {
    PromiseBridge {
      promise.success(callable.call())
    }
  }

  def trySuccess(callable: Callable[T]): Boolean = {
    promise.trySuccess(callable.call())
  }

  def tryCompleteWithBridge(futureBridge: FutureBridge[T]): PromiseBridge[T] = {
    promise.tryCompleteWith(futureBridge.unWrap)
    this
  }

  def futureBridge: FutureBridge[T] = {
    FutureBridge {
      promise.future
    }
  }
}

object Promises {
  def empty[T]: PromiseBridge[T] = {
    PromiseBridge {
      Promise[T]()
    }
  }

  def failed[T](cause: Throwable): PromiseBridge[T] = {
    PromiseBridge {
      Promise.failed[T](cause)
    }
  }

  def successful[T](value: T): PromiseBridge[T] = {
    PromiseBridge {
      Promise.successful(value)
    }
  }

}

