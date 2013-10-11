package org.xblackcat.sjpu.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Additional information for mapping value from .property file to field: default value if property is not defined in .properties file.
 * <p/>
 * 12.02.13 11:24
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DefaultValue {
    /**
     * Default value for the field in string representation.
     */
    String value();
}
