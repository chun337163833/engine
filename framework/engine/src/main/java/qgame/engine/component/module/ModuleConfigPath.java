package qgame.engine.component.module;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ModuleConfigPath{
    String path() default "";
}
