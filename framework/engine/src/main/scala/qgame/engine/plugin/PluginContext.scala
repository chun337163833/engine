package qgame.engine.plugin

import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine

import scala.util.DynamicVariable

/**
 * Created by kerr.
 */
trait PluginContext {
  def engine: Engine
  def config: QGameConfig
  //TODO change the plugin to actor model
}

private[plugin] object PluginContext {
  val stack = new DynamicVariable[List[PluginContext]](Nil)

  case class DefaultPluginContext(engine: Engine, config: QGameConfig) extends PluginContext
}