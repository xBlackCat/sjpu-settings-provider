package org.xblackcat.sjpu.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define target key object class and value object class for parsing property value into map.
 * <p/>
 * 15.10.13 18:27
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MapOf {
    /**
     * Defines a target key object class.
     */
    Class<?> key();

    /**
     * Defines a target value object class.
     */
    Class<?> value();

    /**
     * Sets a custom splitter between key and value. Default is ":"
     */
    String splitter() default ":";
}
