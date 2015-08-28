package qgame.engine.component.module;

import java.lang.annotation.*;

/**
 * Created by kerr.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ServiceDependencies {
    ServiceDependency[] value();
}