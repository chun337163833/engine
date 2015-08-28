package qgame.engine.component

import akka.actor.{ ActorContext, ActorRef, Props }
import qgame.engine.config.QGameConfig
import qgame.engine.core.Engine

import scala.util.DynamicVariable

/**
 * Created by kerr.
 */
trait ComponentContext {
  def actorOf(props: Props): ActorRef

  def actorOf(props: Props, name: String): ActorRef

  def engine: Engine

  def config: QGameConfig
}

private[component] object ComponentContext {
  val stack = new DynamicVariable[List[ComponentContext]](Nil)

  class DefaultComponentContext(
      private val actorContext: ActorContext,
      val engine: Engine,
      val config: QGameConfig
  ) extends ComponentContext {
    def actorOf(props: Props): ActorRef = actorContext.actorOf(props)

    def actorOf(props: Props, name: String): ActorRef = actorContext.actorOf(props, name)
  }

}
