package qgame.engine.component.module

import akka.actor.{ LocalScope, Scope }

/**
 * Created by kerr.
 */
//TODO think more about his
case class ModuleScope(module: Module) extends LocalScope {
  override def withFallback(other: Scope): Scope = module.component.scope
}