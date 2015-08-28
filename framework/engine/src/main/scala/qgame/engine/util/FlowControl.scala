package qgame.engine.util

import akka.actor.{ Actor, ActorRef, Stash }

import scala.concurrent.Future

/**
 * Created by kerr.
 */
trait FlowControl {
  _: Actor with Stash =>

  import FlowControl._

  //enter the flow execution,need save the current behavior
  private val execution: Actor.Receive = {
    case InvokeOnActor(runnable) =>
      runnable.run()
    //    case SwitchTo(receive) =>
    //      context.become(receive, discardOld = false)
    case SwitchBack =>
      context.unbecome()
      unstashAll()
    case msg => stash()
  }

  def flowExecution[T](data: T): FlowExecutionStage[T] = {
    context.become(execution, discardOld = false)
    SyncExecutionStage(data)
  }

  private case class SyncExecutionStage[+T](data: T) extends FlowExecutionStage[T] {

    import scala.concurrent.ExecutionContext.Implicits.global

    def map[U](f: (T) => U): FlowExecutionStage[U] = SyncExecutionStage(f(data))

    def mapAsync[U](f: (T) => Future[U]): AsyncExecutionStage[U] = {
      AsyncExecutionStage(asyncData = f(data))
    }

    def mapParallelism[A, B, U](fa: (T) => Future[A], fb: (T) => Future[B])(f: (A, B) => U): AsyncExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = fa(data)
        val futureB = fb(data)
        for {
          ra <- futureA
          rb <- futureB
          ru = f(ra, rb)
        } yield ru
      }
    }

    def mapParallelism[A, B, C, U](fa: (T) => Future[A], fb: (T) => Future[B], fc: (T) => Future[C])(f: (A, B, C) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = fa(data)
        val futureB = fb(data)
        val futureC = fc(data)
        for {
          ra <- futureA
          rb <- futureB
          rc <- futureC
          ru = f(ra, rb, rc)
        } yield ru
      }
    }

    def mapParallelism[A, B, C, D, U](fa: (T) => Future[A], fb: (T) => Future[B], fc: (T) => Future[C], fd: (T) => Future[D])(f: (A, B, C, D) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = fa(data)
        val futureB = fb(data)
        val futureC = fc(data)
        val futureD = fd(data)
        for {
          ra <- futureA
          rb <- futureB
          rc <- futureC
          rd <- futureD
          ru = f(ra, rb, rc, rd)
        } yield ru
      }
    }

    def mapParallelism[A, B, C, D, E, U](fa: (T) => Future[A], fb: (T) => Future[B], fc: (T) => Future[C], fd: (T) => Future[D], fe: (T) => Future[E])(f: (A, B, C, D, E) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = fa(data)
        val futureB = fb(data)
        val futureC = fc(data)
        val futureD = fd(data)
        val futureE = fe(data)
        for {
          ra <- futureA
          rb <- futureB
          rc <- futureC
          rd <- futureD
          re <- futureE
          ru = f(ra, rb, rc, rd, re)
        } yield ru
      }
    }

    def foreach(f: (T) => Unit): Unit = {
      self ! InvokeOnActor(f(data))
      self ! SwitchBack
    }

    def pipeTo(actorRef: ActorRef): Unit = {
      self ! SwitchBack
      actorRef ! data
    }
  }

  private case class AsyncExecutionStage[+T](asyncData: Future[T]) extends FlowExecutionStage[T] {

    import scala.concurrent.ExecutionContext.Implicits.global

    def map[U](f: (T) => U): FlowExecutionStage[U] = AsyncExecutionStage(asyncData = asyncData.map(f))

    def mapAsync[U](f: (T) => Future[U]): FlowExecutionStage[U] = AsyncExecutionStage(asyncData = asyncData.flatMap(f))

    def mapParallelism[A, B, U](fa: (T) => Future[A], fb: (T) => Future[B])(f: (A, B) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = asyncData.flatMap(fa)
        val futureB = asyncData.flatMap(fb)
        for {
          ra <- futureA
          rb <- futureB
          ru = f(ra, rb)
        } yield ru
      }
    }

    def mapParallelism[A, B, C, U](fa: (T) => Future[A], fb: (T) => Future[B], fc: (T) => Future[C])(f: (A, B, C) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = asyncData.flatMap(fa)
        val futureB = asyncData.flatMap(fb)
        val futureC = asyncData.flatMap(fc)
        for {
          ra <- futureA
          rb <- futureB
          rc <- futureC
          ru = f(ra, rb, rc)
        } yield ru
      }
    }

    def mapParallelism[A, B, C, D, U](fa: (T) => Future[A], fb: (T) => Future[B], fc: (T) => Future[C], fd: (T) => Future[D])(f: (A, B, C, D) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = asyncData.flatMap(fa)
        val futureB = asyncData.flatMap(fb)
        val futureC = asyncData.flatMap(fc)
        val futureD = asyncData.flatMap(fd)
        for {
          ra <- futureA
          rb <- futureB
          rc <- futureC
          rd <- futureD
          ru = f(ra, rb, rc, rd)
        } yield ru
      }
    }

    def mapParallelism[A, B, C, D, E, U](fa: (T) => Future[A], fb: (T) => Future[B], fc: (T) => Future[C], fd: (T) => Future[D], fe: (T) => Future[E])(f: (A, B, C, D, E) => U): FlowExecutionStage[U] = {
      AsyncExecutionStage {
        val futureA = asyncData.flatMap(fa)
        val futureB = asyncData.flatMap(fb)
        val futureC = asyncData.flatMap(fc)
        val futureD = asyncData.flatMap(fd)
        val futureE = asyncData.flatMap(fe)
        for {
          ra <- futureA
          rb <- futureB
          rc <- futureC
          rd <- futureD
          re <- futureE
          ru = f(ra, rb, rc, rd, re)
        } yield ru
      }
    }

    def foreach(f: (T) => Unit): Unit = {
      asyncData.foreach { data =>
        self ! InvokeOnActor(f(data))
        self ! SwitchBack
      }
    }

    def pipeTo(actorRef: ActorRef): Unit = {
      self ! SwitchBack
      asyncData.foreach {
        case data => actorRef ! data
      }
    }
  }
}

object FlowControl {

  sealed trait FlowControlCommand

  object SwitchBack extends FlowControlCommand

  //  case class SwitchTo(receive: Receive) extends FlowControlCommand

  case class InvokeOnActor[T](runnable: Runnable) extends FlowControlCommand

  object InvokeOnActor {
    def apply[T](code: => T): InvokeOnActor[T] = InvokeOnActor(new Runnable {
      def run(): Unit = code
    })
  }

  trait FlowExecutionStage[+T] {
    def map[U](f: T => U): FlowExecutionStage[U]

    def mapAsync[U](f: T => Future[U]): FlowExecutionStage[U]

    def mapParallelism[A, B, U](fa: T => Future[A], fb: T => Future[B])(f: (A, B) => U): FlowExecutionStage[U]

    def mapParallelism[A, B, C, U](fa: T => Future[A], fb: T => Future[B], fc: T => Future[C])(f: (A, B, C) => U): FlowExecutionStage[U]

    def mapParallelism[A, B, C, D, U](fa: T => Future[A], fb: T => Future[B], fc: T => Future[C], fd: T => Future[D])(f: (A, B, C, D) => U): FlowExecutionStage[U]

    def mapParallelism[A, B, C, D, E, U](fa: T => Future[A], fb: T => Future[B], fc: T => Future[C], fd: T => Future[D], fe: T => Future[E])(f: (A, B, C, D, E) => U): FlowExecutionStage[U]

    def foreach(f: (T) => Unit): Unit

    def pipeTo(actorRef: ActorRef): Unit
  }

}
