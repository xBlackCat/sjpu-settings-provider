package org.xblackcat.sjpu.settings.ann;

import org.xblackcat.sjpu.settings.config.IConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Substitute ${property.name} in string values with variable if exists. It is possible to define values of the current interface as well
 * as {@linkplain System#getProperties() system properties}. If the annotation defined in the interface declaration substitution will be
 * applied to all the methods.
 * <p>
 * For substituting values from other configs see {@linkplain org.xblackcat.sjpu.settings.Config#substitute(IConfig)} method
 *
 * @see System#getProperties()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ExpandProperties {
}
