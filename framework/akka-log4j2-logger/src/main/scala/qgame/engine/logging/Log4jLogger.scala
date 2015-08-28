package qgame.engine.logging

import akka.actor.Actor
import akka.event.DummyClassForStringSources
import akka.event.Logging._
import org.apache.logging.log4j.{ LogManager, Logger, ThreadContext }

/**
 * Created by kerr.
 */
class Log4jLogger extends Actor with Log4jLogging {
  private val source = "source"
  private val thread = "thread"
  private val timestamp = "timestamp"

  override def receive: Receive = {
    case event @ Error(cause, logSource, logClass, message) =>
      withThreadContext(logSource, event) {
        cause match {
          case Error.NoCause | null => Log4jLogger(logClass, logSource).error(if (message != null) message.toString else null)
          case _ => Log4jLogger(logClass, logSource).error(if (message != null) message.toString else cause.getLocalizedMessage, cause)
        }
      }
    case event @ Warning(logSource, logClass, message) =>
      withThreadContext(logSource, event) {
        Log4jLogger(logClass, logSource).warn("{}", message.asInstanceOf[AnyRef])
      }
    case event @ Info(logSource, logClass, message) =>
      withThreadContext(logSource, event) {
        Log4jLogger(logClass, logSource).info("{}", message.asInstanceOf[AnyRef])
      }
    case event @ Debug(logSource, logClass, message) =>
      withThreadContext(logSource, event) {
        Log4jLogger(logClass, logSource).debug("{}", message.asInstanceOf[AnyRef])
      }
    case InitializeLogger(_) =>
      log.info("log4j2 InitializeLogger done")
      sender() ! LoggerInitialized
  }

  final def withThreadContext(logSource: String, logEvent: LogEvent)(logStatement: ⇒ Unit): Unit = {
    ThreadContext.put(source, logSource)
    ThreadContext.put(thread, logEvent.thread.getName)
    ThreadContext.put(timestamp, formatTimestamp(logEvent.timestamp))
    logEvent.mdc.foreach {
      case (key, value) => ThreadContext.put(key, String.valueOf(value))
    }
    try {
      logStatement
    } finally {
      ThreadContext.clearAll()
    }
  }

  protected def formatTimestamp(timestamp: Long): String = {
    val timeOfDay = timestamp % 86400000L
    val hours = timeOfDay / 3600000L
    val minutes = timeOfDay / 60000L % 60
    val seconds = timeOfDay / 1000L % 60
    val ms = timeOfDay % 1000
    f"$hours%02d:$minutes%02d:$seconds%02d.$ms%03dUTC"
  }

}

object Log4jLogger {
  sys.props += ("Log4jContextSelector" -> "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
  //  sys.props += ("log4j.Clock" -> "qgame.engine.logging.Log4j2Clock")
  sys.props += ("AsyncLogger.ExceptionHandler" -> "qgame.engine.logging.Log4jExceptionHandler")
  def apply(logger: String): Logger = LogManager.getLogger(logger)
  def apply(logClass: Class[_]): Logger = LogManager.getLogger(logClass)
  def apply(logClass: Class[_], logSource: String): Logger = logClass match {
    case c if c == classOf[DummyClassForStringSources] ⇒ apply(logSource)
    case _ ⇒ apply(logClass)
  }
  def apply(logSource: AnyRef): Logger = LogManager.getLogger(logSource)
  def root: Logger = LogManager.getLogger("engine")
}
