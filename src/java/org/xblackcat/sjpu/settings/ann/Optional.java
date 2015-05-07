package org.xblackcat.sjpu.settings.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Additional information for mapping value from .property file to field: flag for optional property. No exception if field is not defined and no default value is set.
 * <p>
 * If the annotation is applied to interface no exception will be thrown if resource is unavailable for methods
 * {@linkplain org.xblackcat.sjpu.settings.config.AConfig#get(Class, String)} and {@linkplain org.xblackcat.sjpu.settings.config.AConfig#get(Class)}
 * <p>
 * 12.02.13 11:24
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Optional {
}
