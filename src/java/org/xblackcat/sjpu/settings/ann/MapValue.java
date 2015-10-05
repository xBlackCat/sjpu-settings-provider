package org.xblackcat.sjpu.settings.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define target map value object class for parsing property value into map.
 * <p>
 * 15.10.13 18:27
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MapValue {
    /**
     * @return Defines a target value object class.
     */
    Class<?> value();
}
