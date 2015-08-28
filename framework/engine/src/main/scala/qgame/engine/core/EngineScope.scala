package qgame.engine.core

import akka.actor.{ Scope, LocalScope }

/**
 * Created by kerr.
 */
case class EngineScope(engine: Engine) extends LocalScope {
  override def withFallback(other: Scope): Scope = this
}
