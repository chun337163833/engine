package qgame.engine.logging

import akka.event.DummyClassForStringSources
import org.apache.logging.log4j.{ LogManager, Logger }

/**
 * Created by kerr.
 */
//TODO switch to MemoryMappedFileAppender
object Log4j2Logger {
  sys.props += ("Log4jContextSelector" -> "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
  sys.props += ("log4j.Clock" -> "qgame.engine.logging.Log4j2Clock")
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
