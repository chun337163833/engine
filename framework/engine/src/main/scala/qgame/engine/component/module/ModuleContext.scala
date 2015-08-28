package qgame.engine.component.module

import akka.actor.{ ActorContext, ActorRef, Props }
import qgame.engine.component.Component
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine

import scala.util.DynamicVariable

/**
 * Created by kerr.
 */
trait ModuleContext {
  def actorOf(props: Props): ActorRef

  def actorOf(props: Props, name: String): ActorRef

  def engine: Engine

  def component: Component

  def config: QGameConfig

  def name: String

  def getExternalConfig(key: String): QGameConfig
}

private[module] object ModuleContext {
  val stack = new DynamicVariable[List[ModuleContext]](Nil)

  private[module] case class DefaultModuleContext(
      private val clazz: Class[_ <: Module],
      private val actorContext: ActorContext,
      engine: Engine,
      component: Component,
      config: QGameConfig,
      name: String
  ) extends ModuleContext {
    override def actorOf(props: Props): ActorRef = actorContext.actorOf(props)

    override def actorOf(props: Props, name: String): ActorRef = actorContext.actorOf(props, name)

    override def getExternalConfig(key: String): QGameConfig = ExternalConfig(clazz, key, engine.classLoader)
  }

}
