package qgame.engine.libs;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by kerr.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JsonTypeInfo(use = Id.MINIMAL_CLASS,include = As.EXTERNAL_PROPERTY,property = "@t")
@JacksonAnnotationsInside
public @interface MiniTypeInfo {
}
