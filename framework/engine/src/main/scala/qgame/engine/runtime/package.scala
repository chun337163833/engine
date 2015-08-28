package qgame.engine

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinPool.ManagedBlocker
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.{ CanAwait, BlockContext, ExecutionContext }

/**
 * Created by kerr.
 */
package object runtime {
  implicit val globalExecutionContext: RuntimeExecutionContext = new DefaultRuntimeExecutionContext

  abstract class RuntimeExecutionContext extends ExecutionContext with BlockContext

  private class DefaultRuntimeExecutionContext extends RuntimeExecutionContext {
    import DefaultRuntimeExecutionContext.commonPool
    override def execute(runnable: Runnable): Unit = commonPool.execute(runnable)

    override def reportFailure(cause: Throwable): Unit = {
      cause.printStackTrace()
    }

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

  private object DefaultRuntimeExecutionContext {
    private val commonPool = ForkJoinPool.commonPool()
  }
}
