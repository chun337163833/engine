package qgame.engine.core

import akka.actor.{ ActorSystem, ActorContext, ActorRef, Props }
import qgame.engine.config.QGameConfig
import qgame.engine.runtime.EngineRuntime

import scala.util.DynamicVariable

/**
 * Created by kerr.
 */
trait EngineContext {
  def name: String

  def actorSystem: ActorSystem

  def actorOf(props: Props): ActorRef

  def actorOf(props: Props, name: String): ActorRef

  private[engine] def stop(actorRef: ActorRef): Unit

  def runtime: EngineRuntime

  def config: QGameConfig
}

private[engine] object EngineContext {
  val stack = new DynamicVariable[List[EngineContext]](Nil)
}

private[engine] case class DefaultEngineContext(
    private val actorContext: ActorContext,
    name: String,
    actorSystem: ActorSystem,
    runtime: EngineRuntime
) extends EngineContext {
  override def actorOf(props: Props): ActorRef = actorContext.actorOf(props)

  override def actorOf(props: Props, name: String): ActorRef = actorContext.actorOf(props, name)

  override private[engine] def stop(actorRef: ActorRef): Unit = actorContext.stop(actorRef)

  override def config: QGameConfig = runtime.engineConfig
}

