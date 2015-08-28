package qgame.engine.component.module

import akka.actor.SupervisorStrategy.{ Resume, Stop }
import akka.actor._
import akka.event.LoggingAdapter

import scala.util.control.NonFatal

/**
 * Created by kerr.
 */
trait ModuleRuntimeProps {
  def dispatcherClazz: String
  def mailboxClazz: String
  def supervisorStrategyProvider: LoggingAdapter => SupervisorStrategy
}

object ModuleRuntimeProps {
  val defaultDispatcherClazz = ""
  val defaultMailboxClazz = ""
}

case class ConfigurableModuleRuntimeProps(
  dispatcherClazz: String = ModuleRuntimeProps.defaultDispatcherClazz,
  mailboxClazz: String = ModuleRuntimeProps.defaultMailboxClazz,
  supervisorStrategyProvider: LoggingAdapter => SupervisorStrategy
) extends ModuleRuntimeProps

object DefaultModuleRuntimeProps extends ModuleRuntimeProps {
  override def dispatcherClazz: String = ModuleRuntimeProps.defaultDispatcherClazz

  override def supervisorStrategyProvider: LoggingAdapter => SupervisorStrategy = moduleRuntimeLogging =>
    OneForOneStrategy(maxNrOfRetries = Int.MaxValue) {
      case _: ActorInitializationException ⇒ Stop
      case _: ActorKilledException ⇒ Stop
      case _: DeathPactException ⇒ Stop
      case NonFatal(e) =>
        moduleRuntimeLogging.error(e, "module runtime error,nonFatal,Resume")
        Resume
      case fatal =>
        Stop
    }

  override def mailboxClazz: String = ModuleRuntimeProps.defaultMailboxClazz
}

trait SuperVisorStrategyProvider {
  def create(moduleRuntimeLogging: LoggingAdapter): SupervisorStrategy
}