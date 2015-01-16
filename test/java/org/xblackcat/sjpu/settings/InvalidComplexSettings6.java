package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.MapValue;

import java.util.Map;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings6 {
    @MapValue(Double.class)
    Map<Long, CharSequence> wrongAnnotated();
}
