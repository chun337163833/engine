package qgame.engine.component;

import java.lang.annotation.*;

/**
 * Created by kerr.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ComponentConfigPath {
    String path() default "";
}
