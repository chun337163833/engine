package gear.sample.engine.component.io

import akka.actor.{ ActorRef, Props }
import qgame.engine.component.AbstractComponent
import qgame.engine.component.Component.{ ModuleBinder, ModuleBinding }
import qgame.engine.config.QGameConfig

/**
 * Created by kerr.
 */
class IOComponent extends AbstractComponent {
  private var binderHandler: ActorRef = _
  private var clientHandler: ActorRef = _
  override protected def preStart(): Unit = {
    binderHandler = context.actorOf(Props.create(classOf[ServerActor]))
    clientHandler = context.actorOf(Props.create(classOf[ClientActor]))
    super.preStart()
  }

  override protected def postStop(): Unit = super.postStop()

  override protected def loadModuleBindings(binder: ModuleBinder, config: QGameConfig): List[ModuleBinding] = binder.build
}
