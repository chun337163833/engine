package qgame.engine.component.module;

import qgame.engine.core.Engine;
import qgame.engine.config.QGameConfig;
import scala.concurrent.Promise;

/**
 * Created by kerr.
 */
@ModuleConfigPath
public interface Module2 {
     ModuleContext context();

     Engine getEngine();

     QGameConfig getConfig();

     void init(Engine engine,ModuleContext context);

     void start(Promise<String> completePromise);

     void stop(Promise<String> promise);

     Boolean isHA();
}

