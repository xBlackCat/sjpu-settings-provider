package org.xblackcat.sjpu.settings.ann;

import org.xblackcat.sjpu.settings.converter.IParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a parser class for converting a string to a custom user object. The converter is affected on scalar return type, arrays and
 * collections. Maps return types not supported
 * <p>
 * 15.10.13 18:27
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ParseWith {
    /**
     * @return  Defines a target map key object class.
     */
    Class<? extends IParser<?>> value();
}
