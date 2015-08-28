package qgame.engine.libs

import java.lang.Iterable
import java.util.concurrent.{ Callable, TimeUnit }

import akka.actor.{ ActorRef, ActorSelection, Scheduler, Status }
import akka.pattern.CircuitBreaker
import qgame.akka.extension.netty.Netty
import qgame.engine.libs.Functions.{ Consumer1, Function1, Function2, Predicate1 }
import qgame.engine.util._

import scala.collection.JavaConverters
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

/**
 * Created by kerr.
 */
object FutureBridge {
  def apply[T](future: Future[T]): FutureBridge[T] = new FutureBridge[T](future)
}

class FutureBridge[T](private val future: Future[T]) {

  def unWrap: Future[T] = future

  def andThen(thenHandler: ThenHandler[T]): FutureBridge[T] = {
    FutureBridge {
      future.andThen {
        case Success(v) =>
          thenHandler.onSuccess(v)
        case Failure(e) =>
          thenHandler.OnFailure(e)
      }
    }
  }

  def andThen(onSuccess: Consumer1[T], onFailure: Consumer1[Throwable]): FutureBridge[T] = {
    FutureBridge {
      future.andThen {
        case Success(v) =>
          onSuccess.accept(v)
        case Failure(e) =>
          onFailure.accept(e)
      }
    }
  }

  def failed: FutureBridge[Throwable] = {
    FutureBridge {
      future.failed
    }
  }

  def fallbackTo[U >: T](that: Future[U]): FutureBridge[U] = {
    FutureBridge {
      future.fallbackTo(that)
    }
  }

  def filter(predicate: Predicate1[T]): FutureBridge[T] = {
    FutureBridge {
      future.filter(predicate.test)
    }
  }

  def flatMap[U](mapper: Function1[T, Future[U]]): FutureBridge[U] = {
    FutureBridge {
      import scala.concurrent.ExecutionContext.Implicits.global
      future.flatMap(mapper.apply)
    }
  }

  def foreach(handler: Consumer1[T]): Unit = {
    future.foreach(handler.accept)
  }

  def isComplete: Boolean = {
    future.isCompleted
  }

  def map[U](mapper: Function1[T, U]): FutureBridge[U] = {
    FutureBridge {
      future.map(mapper.apply)
    }
  }

  def mapTo[U](clazz: Class[U]): FutureBridge[U] = {
    FutureBridge {
      future.map {
        value =>
          if (clazz.isInstance(value)) {
            value.asInstanceOf[U]
          } else {
            throw new ClassCastException(s"$value is not a instance of $clazz")
          }
      }
    }
  }

  def onComplete(completeHandler: CompleteHandler[T]): Unit = {
    future.onComplete {
      case Success(v) =>
        completeHandler.onSuccess(v)
      case Failure(e) =>
        completeHandler.OnFailure(e)
    }(scala.concurrent.ExecutionContext.Implicits.global)
  }

  def onComplete(onSuccess: Consumer1[T], onFailure: Consumer1[Throwable]): Unit = {
    future.onComplete {
      case Success(v) =>
        onSuccess.accept(v)
      case Failure(e) =>
        onFailure.accept(e)
    }(scala.concurrent.ExecutionContext.Implicits.global)
  }

  def onFailure(handler: Consumer1[Throwable]): Unit = {
    future.onFailure {
      case NonFatal(e) =>
        handler.accept(e)
    }
  }

  def onSuccess(handler: Consumer1[T]): Unit = {
    future.onSuccess {
      case value => handler.accept(value)
    }
  }

  def recover[U >: T](recover: Function1[Throwable, U]): FutureBridge[U] = {
    FutureBridge {
      future.recover {
        case NonFatal(e) =>
          recover.apply(e)
      }
    }
  }

  def recoverWith[U >: T](recover: Function1[Throwable, Future[U]]): FutureBridge[U] = {
    FutureBridge {
      future.recoverWith {
        case NonFatal(e) =>
          recover.apply(e)
      }
    }
  }

  def transform[U](transformer: Transformer[T, U]): FutureBridge[U] = {
    FutureBridge {
      future.transform(transformer.onSuccess, transformer.OnFailure)
    }
  }

  def transform[U](transSuccess: Function1[T, U], transFailure: Function1[Throwable, Throwable]): FutureBridge[U] = {
    FutureBridge {
      future.transform(transSuccess.apply, transFailure.apply)
    }
  }

  def value: T = {
    future.value.map {
      case Success(v) => v
      case Failure(e) =>
        throw e
    }.getOrElse(null.asInstanceOf[T])
  }

  def zip[U](that: Future[U]): FutureBridge[(T, U)] = {
    FutureBridge {
      future.zip(that)
    }
  }

  @throws(classOf[TimeoutException])
  @throws(classOf[InterruptedException])
  def await(length: Long, timeUnit: TimeUnit): FutureBridge[T] = {
    FutureBridge {
      Await.ready(future, Duration(length, timeUnit))
    }
  }

  @throws(classOf[Exception])
  def result(length: Long, timeUnit: TimeUnit): T = {
    Await.result(future, Duration(length, timeUnit))
  }

  def pipe(recipient: ActorSelection): Unit = {
    future.onComplete {
      case Success(v) => recipient ! v
      case Failure(e) => recipient ! Status.Failure(e)
    }
  }

  def pipe(recipient: ActorRef): Unit = {
    future.onComplete {
      case Success(v) => recipient ! v
      case Failure(e) => recipient ! Status.Failure(e)
    }
  }

  def maxWait(length: Long, timeUnit: TimeUnit)(scheduler: Scheduler): FutureBridge[T] = {
    FutureBridge {
      val maxWaitPromise = Promise[T]()
      maxWaitPromise.completeWith(future)
      val duration = Duration(length, timeUnit)
      scheduler.scheduleOnce(duration) {
        maxWaitPromise.tryFailure(new TimeoutException(s"max time out wait after $duration"))
      }
      maxWaitPromise.future
    }
  }

  def withCircuitBreaker(circuitBreaker: CircuitBreaker): FutureBridge[T] = {
    FutureBridge {
      circuitBreaker.withCircuitBreaker(future)
    }
  }
}

object Futures {
  def wrap[U](future: io.netty.util.concurrent.Future[U]): FutureBridge[U] = wrap(Netty.NettyFutureBridge.proxy(future))

  def wrap[U](future: Future[U]): FutureBridge[U] = FutureBridge(future)

  def future[U](u: U): FutureBridge[U] = {
    FutureBridge {
      Future(u)
    }
  }

  def future[U](callable: Callable[U]): FutureBridge[U] = {
    FutureBridge {
      Future(callable.call())
    }
  }

  def failed[U](exception: Throwable): FutureBridge[U] = {
    FutureBridge {
      Future.failed[U](exception)
    }
  }

  def find[U](futures: Iterable[Future[U]], predicate: Predicate1[U]): FutureBridge[Option[U]] = {
    FutureBridge {
      import scala.collection.JavaConversions._
      Future.find(futures)(predicate.test)
    }
  }

  def firstCompletedOf[U](futures: Iterable[Future[U]]): FutureBridge[U] = {
    FutureBridge {
      import scala.collection.JavaConversions._
      Future.firstCompletedOf(futures)
    }
  }

  def fold[R, T](futures: Iterable[Future[T]], folder: Function2[R, T, R])(zero: R): FutureBridge[R] = {
    FutureBridge {
      import scala.collection.JavaConversions._
      Future.fold(futures)(zero)(folder.apply)
    }
  }

  def reduce[T, R >: T](futures: Iterable[Future[T]], reducer: Function2[R, T, R]): FutureBridge[R] = {
    FutureBridge {
      import scala.collection.JavaConversions._
      //      Future.reduce(futures.toList)(reducer.apply)
      if (futures.isEmpty)
        Future.failed(new NoSuchElementException("reduce attempted on empty collection"))
      Future.sequence(futures.toList).map(_ reduceLeft reducer.apply)
    }
  }

  def sequence[U](futures: Iterable[Future[U]]): FutureBridge[Iterable[U]] = {
    FutureBridge {
      import scala.collection.JavaConversions._
      Future.sequence(futures.toList).map {
        results =>
          JavaConverters.asJavaIterableConverter(results).asJava
      }
    }
  }

  def traverse[U, I](inputs: Iterable[U], mapper: Function1[U, Future[I]]): FutureBridge[Iterable[I]] = {
    FutureBridge {
      import scala.collection.JavaConversions._
      Future.traverse(inputs.toList)(mapper.apply).map {
        results =>
          JavaConverters.asJavaIterableConverter(results).asJava
      }
    }
  }

  def block[U](callable: Callable[U]): U = {
    blocking {
      callable.call()
    }
  }

  def after[U](length: Long, timeUnit: TimeUnit)(scheduler: Scheduler)(callable: Callable[Future[U]]): FutureBridge[U] = {
    FutureBridge {
      val duration = Duration(length, timeUnit)
      if (duration.isFinite() && duration.length < 1) {
        try callable.call() catch {
          case NonFatal(t) ⇒ Future.failed(t)
        }
      } else {
        val p = Promise[U]()
        scheduler.scheduleOnce(duration) {
          p completeWith {
            try callable.call() catch {
              case NonFatal(t) ⇒ Future.failed(t)
            }
          }
        }
        p.future
      }
    }
  }
}
