package gear.sample.engine.component.ping

import gear.sample.engine.component.ping.module.PingServiceModule
import qgame.engine.component.{ ComponentConfigPath, AbstractComponent }
import qgame.engine.component.Component.{ ModuleBinding, ModuleBinder }
import qgame.engine.component.module.ModuleProps
import qgame.engine.config.QGameConfig

/**
 * Created by kerr.
 */
@ComponentConfigPath(path = "ping")
class PingComponent extends AbstractComponent {
  override protected def loadModuleBindings(binder: ModuleBinder, config: QGameConfig): List[ModuleBinding] = {
    binder.bind("ping-module", ModuleProps.create(classOf[PingServiceModule])).build
  }
}
