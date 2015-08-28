package qgame.engine.runtime

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.ForkJoinPool.{ ManagedBlocker, ForkJoinWorkerThreadFactory }
import java.util.concurrent.atomic.{ AtomicReference, AtomicInteger }
import java.util.concurrent.{ ForkJoinTask, ForkJoinPool, ForkJoinWorkerThread }

import qgame.engine.libs.LoggingAble

import scala.concurrent.{ CanAwait, BlockContext, ExecutionContext }
import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
private[runtime] class EngineExecutionContext(parallelism: Int, asyncMode: Boolean) extends ExecutionContext with LoggingAble {
  import EngineExecutionContext._
  private val forkJoinPool = new EngineForkJoinPool(
    math.max(parallelism, Runtime.getRuntime.availableProcessors()),
    forkJoinWorkerThreadFactory,
    uncaughtExceptionHandler,
    asyncMode
  )

  override def execute(runnable: Runnable): Unit = {
    forkJoinPool.execute(runnable)
  }

  override def reportFailure(cause: Throwable): Unit = {
    log.error(cause, "failure detected on engine execution context")
  }
}

private[runtime] object EngineExecutionContext {

  private val forkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory("EngineExecutionContext")
  private val uncaughtExceptionHandler = new DefaultUncaughtExceptionHandler

  class EngineForkJoinPool(
      parallelism: Int,
      factory: ForkJoinPool.ForkJoinWorkerThreadFactory,
      handler: Thread.UncaughtExceptionHandler,
      asyncMode: Boolean
  ) extends ForkJoinPool(parallelism, factory, handler, asyncMode) {

    override def execute(task: Runnable): Unit = {
      if (task == null) throw new NullPointerException
      val job: ForkJoinTask[_] = task match {
        case forkJoinTask: ForkJoinTask[_] =>
          forkJoinTask
        case runnable => new RunnableWrapper(runnable)
      }
      //      Thread.currentThread() match {
      //        case worker: ForkJoinWorkerThread if worker.getPool eq this => job.fork()
      //        case _ => this.execute(job)
      //      }
      super.execute(job)
    }
  }

  private class RunnableWrapper(runnable: Runnable) extends ForkJoinTask[Unit] with LoggingAble {
    override def exec(): Boolean = try {
      runnable.run()
      true
    } catch {
      case ie: InterruptedException ⇒
        Thread.currentThread.interrupt()
        false
      case NonFatal(e) =>
        log.error(e, "nonfatal exception on engine forkjoin pool")
        throw e
    }

    override def getRawResult: Unit = ()

    override def setRawResult(value: Unit): Unit = ()
  }

  private class DefaultForkJoinWorkerThreadFactory(name: String) extends ForkJoinWorkerThreadFactory {
    private val counter = new AtomicInteger(0)

    def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
      new DefaultForkJoinWorkerThread(s"$name-${counter.incrementAndGet()}", pool)
    }
  }

  private class DefaultForkJoinWorkerThread(name: String, pool: ForkJoinPool) extends ForkJoinWorkerThread(pool) with BlockContext {
    super.setName(name)

    override def blockOn[T](thunk: => T)(implicit permission: CanAwait): T = {
      val ref = new AtomicReference[Option[T]](None)
      ForkJoinPool.managedBlock(new ManagedBlocker {
        override def isReleasable: Boolean = ref.get().isDefined

        override def block(): Boolean = {
          ref.lazySet(Some(thunk))
          true
        }
      })
      ref.get().get
    }
  }

  private class DefaultUncaughtExceptionHandler extends UncaughtExceptionHandler {
    override def uncaughtException(t: Thread, e: Throwable): Unit = ()
  }
}
