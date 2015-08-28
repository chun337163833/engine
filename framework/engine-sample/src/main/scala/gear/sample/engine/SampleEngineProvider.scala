package gear.sample.engine

import qgame.engine.core.Engine
import qgame.engine.ignition.EngineProvider

/**
 * Created by kerr.
 */
class SampleEngineProvider extends EngineProvider {
  /**
   * just create the engine
   * with the engine bootstrap ,using engine.start to start up the engine,and with engine.stop to stop it
   */
  override def get(): Engine = {
    new SampleEngine
  }
}
