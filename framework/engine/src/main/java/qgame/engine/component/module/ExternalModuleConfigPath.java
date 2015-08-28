package qgame.engine.component.module;

import java.lang.annotation.*;

/**
 * Created by kerr.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ExternalModuleConfigPaths.class)
public @interface ExternalModuleConfigPath {
    String key();
    String target();
}

