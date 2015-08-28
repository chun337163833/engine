package qgame.engine.component.module;

import qgame.engine.core.Engine;


/**
 * Created by DongLei on 2014/10/24.
 */
public interface ModuleCreater {
    public Module2 create(Engine engine,String id,ModuleArgs moduleArgs);
}

