package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.PropertyName;

/**
 * 14.10.13 16:55
 *
 * @author xBlackCat
 */
public interface Settings2 {
    String getValue();

    @PropertyName("value2")
    String getAnotherValue();
}
