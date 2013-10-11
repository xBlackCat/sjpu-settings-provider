package org.xblackcat.sjpu.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Default resource name for loading settings from.
 *
 * 12.02.13 11:23
 *
 * @author xBlackCat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SettingsSource {
    String value();
}
