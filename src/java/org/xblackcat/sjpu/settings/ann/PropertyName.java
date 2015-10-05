package org.xblackcat.sjpu.settings.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Additional information for mapping value from .property file to field: custom property name
 * <p>
 * 12.02.13 11:24
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface PropertyName {
    /**
     * Defines a property name to be mapped to the method. If a prefix defined with {@linkplain Prefix}
     * annotation it will be added at the beginning of the property name. By default property name will be generated from
     * annotated method name as follow:
     * <ul>
     * <li><code>getMyPropertyValue() -&gt; my.property.value</code></li>
     * <li><code>isMyPropertyValue() -&gt; my.property.value</code></li>
     * <li><code>myPropertyValue() -&gt; my.property.value</code></li>
     * <li><code>getMyPropertyValue() -&gt; my.property.value</code></li>
     * </ul>
     *
     * @return customized property name
     */
    String value();
}
