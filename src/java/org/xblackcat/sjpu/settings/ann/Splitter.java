package org.xblackcat.sjpu.settings.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets a custom splitter between key and value for {@linkplain java.util.Map Map} return type. Default key-value splitter is ":"
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Splitter {
    String value();
}
