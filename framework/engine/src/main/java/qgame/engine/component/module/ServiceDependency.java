package qgame.engine.component.module;

import java.lang.annotation.*;

/**
 * Created by kerr.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ServiceDependencies.class)
@Inherited
public @interface ServiceDependency {
    String name();

    String version() default "0.0.1";

    Scope scope() default Scope.REQUIRED;

    enum Scope {
        REQUIRED,//must need for start the module
        RUNTIME//will provided by engine at runtime
    }

    RequiredPolicy requirePolicy() default RequiredPolicy.RETRY;

    int maxWaitInSeconds() default 10;

    int maxRetry() default 1;

    enum RequiredPolicy {
        FAIL,
        IGNORE,
        RETRY
    }

}
