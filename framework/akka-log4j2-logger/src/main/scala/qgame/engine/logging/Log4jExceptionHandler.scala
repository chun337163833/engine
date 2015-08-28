package qgame.engine.logging

import com.lmax.disruptor.ExceptionHandler

/**
 * Created by kerr.
 */
class Log4jExceptionHandler extends ExceptionHandler[Object] {
  override def handleEventException(ex: Throwable, sequence: Long, event: Object): Unit = {
    println("exception when log4j logging event")
    ex.printStackTrace()
  }

  override def handleOnShutdownException(ex: Throwable): Unit = {
    println("exception when log4j shutdown")
    ex.printStackTrace()
  }

  override def handleOnStartException(ex: Throwable): Unit = {
    println("exception when log4j start")
    ex.printStackTrace()
  }
}
