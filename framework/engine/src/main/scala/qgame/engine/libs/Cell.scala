package qgame.engine.libs

import scala.annotation.tailrec
import scala.concurrent.stm.{ Ref, Txn }
import scala.concurrent.{ ExecutionContext, Future, Promise }

/**
 * Created by kerr.
 */
case class Cell[V](initialValue: V) {
  private val ref: Ref[V] = Ref(initialValue)

  def value: V = ref.single.get

  def set(value: V): Unit = {
    ref.single.set(value)
  }

  def lazySet(value: V): Unit = {
    CellHelper.lazySet(value)
  }

  def update(value: V): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    CellHelper.update(value)
  }

  def transform(f: V => V): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    CellHelper.transform(f)
  }

  def updateAsync(value: V): Future[V] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    CellHelper.updateAsync(value)
  }

  def transformAsync(f: V => V): Future[V] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    CellHelper.transformAsync(f)
  }

  private object CellHelper {
    private def inTransaction(runnable: Runnable)(implicit ex: ExecutionContext): Unit = {
      Txn.findCurrent match {
        case None =>
          ex.execute(runnable)
        case Some(txn) =>
          Txn.afterCommit(_ => ex.execute(runnable))(txn)
      }
    }

    def update(value: V)(implicit ex: ExecutionContext): Unit = {
      inTransaction(new Runnable {
        override def run(): Unit = ref.single.update(value)
      })
    }

    @tailrec
    def lazySet(value: V): Boolean = {
      val old = ref.single.get
      if (ref.single.compareAndSet(old, value)) {
        true
      } else {
        lazySet(value)
      }
    }

    def transform(f: V => V)(implicit ex: ExecutionContext): Unit = {
      inTransaction(new Runnable {
        override def run(): Unit = ref.single.transform(f)
      })
    }

    def updateAsync(value: V)(implicit ex: ExecutionContext): Future[V] = {
      val promise = Promise[V]()
      inTransaction(new Runnable {
        override def run(): Unit = {
          ref.single.update(value)
          promise.success(value)
        }
      })
      promise.future
    }

    def transformAsync(f: V => V)(implicit ex: ExecutionContext): Future[V] = {
      val promise = Promise[V]()
      inTransaction(new Runnable {
        override def run(): Unit = {
          promise.success(ref.single.transformAndGet(f))
        }
      })
      promise.future
    }

  }

}

