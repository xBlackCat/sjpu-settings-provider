package org.xblackcat.sjpu.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Additional information for mapping value from .property file to field. If field is not annotated with the annotation the default values
 * will be used to generate mapping. See the annotation fields description for details.
 * <p/>
 * 12.02.13 11:24
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SettingsField {
    /**
     * Defines a property name to be mapped to the method. If a prefix defined with {@linkplain org.xblackcat.sjpu.settings.SettingsPrefix}
     * annotation it will be added at the beginning of the property name. By default property name will be generated from
     * annotated method name as follow:
     * <ul>
     * <li><code>getMyPropertyValue() -&gt; my.property.value</code></li>
     * <li><code>isMyPropertyValue() -&gt; my.property.value</code></li>
     * <li><code>myPropertyValue() -&gt; my.property.value</code></li>
     * <li><code>getMyPropertyValue() -&gt; my.property.value</code></li>
     * </ul>
     */
    String value() default "";

    /**
     * Default value for the field in string representation. "" means no default value.
     */
    String defaultValue() default "";

    /**
     * Non-required fields initializes with null value if correspond property is not found.
     */
    boolean required() default true;
}
