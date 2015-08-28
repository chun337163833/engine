package qgame.engine.core.immutable;

import qgame.engine.core.serialization.EngineMessage;

/**
 *
 * Created by DongLei on 2015/4/20.
 */
public abstract class Pojo implements EngineMessage{
    private static final long serialVersionUID = 6711920342881364551L;
    public abstract Pojo copyAndReset();
}
