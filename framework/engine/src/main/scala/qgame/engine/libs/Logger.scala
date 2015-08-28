package qgame.engine.libs

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import org.apache.logging.log4j.{ Logger => Log4jLogger }
import qgame.engine.core.Engine
import qgame.engine.logging.Log4j2Logger

/**
 * Created by kerr.
 */
object Logger {
  private var maybeEngine: Option[Engine] = None
  def init(engine: Engine) = {
    maybeEngine = Option(engine)
  }

  def logger(clazz: Class[_]): LoggingAdapter = apply(clazz)

  def logger = {
    maybeEngine.map {
      engine =>
        Logging(engine.actorSystem, engine.name)
    }.getOrElse(PrintLoggingAdapter(""))
  }

  def apply(actorSystem: ActorSystem, clazz: Class[_]): LoggingAdapter = Logging(actorSystem, clazz)

  def apply(clazz: Class[_]): LoggingAdapter = {
    maybeEngine.map {
      engine =>
        Logging(engine.actorSystem, clazz)
    }.getOrElse(PrintLoggingAdapter(clazz))
  }

  def apply(source: AnyRef): LoggingAdapter = {
    maybeEngine.map {
      engine =>
        Logging.getLogger(engine.actorSystem, source)
    }.getOrElse(PrintLoggingAdapter(source))
  }

  def apply(source: String): LoggingAdapter = {
    maybeEngine.map {
      engine =>
        Logging.apply(engine.actorSystem, source)
    }.getOrElse(PrintLoggingAdapter(source))
  }
}

abstract class LoggerProxy extends LoggingAdapter {
  def logger: Log4jLogger
  import qgame.engine.logging.ConsoleColor._
  override def isErrorEnabled: Boolean = true

  override def isInfoEnabled: Boolean = true

  override def isDebugEnabled: Boolean = true

  override def isWarningEnabled: Boolean = true

  override protected def notifyError(message: String): Unit = {
    println(s"[${red("Error")}]: $message\n   --> : ${logger.getName}")
    logger.error(message)
  }

  override protected def notifyError(cause: Throwable, message: String): Unit = {
    println(s"[${red("Error")}]: $message\n   --> : ${logger.getName}")
    if (cause ne null) {
      cause.printStackTrace()
    }
    logger.error(message, cause)
  }

  override protected def notifyInfo(message: String): Unit = {
    println(s"[${green("Info")}] : $message\n   --> : ${logger.getName}")
    logger.info(message)
  }

  override protected def notifyWarning(message: String): Unit = {
    println(s"[${yellow("Warn")}] : $message\n   --> : ${logger.getName}")
    logger.warn(message)
  }

  override protected def notifyDebug(message: String): Unit = {
    println(s"[${blue("Debug")}]: $message\n   --> : ${logger.getName}")
    logger.debug(message)
  }

}

case class PrintLoggingAdapter(logger: Log4jLogger) extends LoggerProxy

object PrintLoggingAdapter {
  def apply(source: String): PrintLoggingAdapter = {
    val logger = Log4j2Logger(source)
    PrintLoggingAdapter(logger)
  }

  def apply(source: Class[_]): PrintLoggingAdapter = {
    val logger = Log4j2Logger(source)
    PrintLoggingAdapter(logger)
  }

  def apply(source: AnyRef): PrintLoggingAdapter = {
    val logger = Log4j2Logger(source)
    PrintLoggingAdapter(logger)
  }
}

trait LoggingAble {
  private var loggingAdapter: LoggingAdapter = _

  def log: LoggingAdapter = {
    if (loggingAdapter eq null)
      loggingAdapter = Logger(this)
    loggingAdapter
  }
}
