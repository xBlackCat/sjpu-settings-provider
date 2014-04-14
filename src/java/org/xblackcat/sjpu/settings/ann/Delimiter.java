package org.xblackcat.sjpu.settings.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set custom separator for {@linkplain ListOf @ListOf}, {@linkplain SetOf @SetOf}
 * and {@linkplain MapOf @MapOf} annotations
 * <p/>
 * 12.02.13 11:24
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Delimiter {
    String value();
}
