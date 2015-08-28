package qgame.engine.logging

import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.Actor
import akka.event.Logging
import akka.event.Logging.{ Error, InitializeLogger, LogEvent, LoggerInitialized }

import scala.util.control.NoStackTrace

/**
 * Created by kerr.
 */
class ConsoleLogger extends Actor {
  import ConsoleColor._
  private val date = new Date
  private val formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  override def receive: Receive = {
    case event: LogEvent =>
      println(convert(event))
    case InitializeLogger(_) =>
      println(s"[${cyan("ConsoleLogger")}]:InitializeLogger done")
      sender() ! LoggerInitialized
    case msg =>
      println(s"[${white("Unknown Message :")}]:$msg")
  }

  /**
   * Returns the StackTrace for the given Throwable as a String
   */
  def stackTraceFor(e: Throwable): String = e match {
    case null | Error.NoCause ⇒ ""
    case _: NoStackTrace ⇒ " (" + e.getClass.getName + ")"
    case other ⇒
      val sw = new java.io.StringWriter
      val pw = new java.io.PrintWriter(sw)
      pw.append('\n')
      other.printStackTrace(pw)
      sw.toString
  }

  def convert(event: LogEvent): String = {
    date.setTime(event.timestamp)
    val timeStamp = formater.format(date)
    event.level match {
      case Logging.ErrorLevel =>
        s"[${red("Error")}][$timeStamp]: ${event.message}\n${stackTraceFor(event.asInstanceOf[Error].cause)}\n   --> : ${event.logSource}"
      case Logging.WarningLevel =>
        s"[${yellow("Warn")}][$timeStamp] : ${event.message}\n   --> : ${event.logSource}"
      case Logging.InfoLevel =>
        s"[${green("Info")}][$timeStamp] : ${event.message}\n   --> : ${event.logSource}"
      case Logging.DebugLevel =>
        s"[${blue("Debug")}][$timeStamp]: ${event.message}\n   --> : ${event.logSource}"
      case _ =>
        s"[${magenta("Unknown")}][$timeStamp]: ${event.message}\n   --> : ${event.logSource}"
    }
  }

}

