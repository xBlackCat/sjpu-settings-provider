package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.MapKey;

import java.util.Map;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings7 {
    @MapKey(String.class)
    Map<Number, String> wrongAnnotated();
}
