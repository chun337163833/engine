package qgame.engine.component

import akka.actor.{ Scope, LocalScope }

/**
 * Created by kerr.
 */
case class ComponentScope(component: Component) extends LocalScope {
  override def withFallback(other: Scope): Scope = component.engine.scope
}
