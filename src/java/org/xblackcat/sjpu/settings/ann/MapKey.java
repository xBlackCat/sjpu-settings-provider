package org.xblackcat.sjpu.settings.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define target key object class for parsing property value into map.
 * <p>
 * 15.10.13 18:27
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MapKey {
    /**
     * @return Defines a target map key object class.
     */
    Class<?> value();
}
