package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.Optional;

/**
 * Optional settings with not filled a value. Should be null in {@linkplain IOptionalSubSettings#getSubSettings()}
 *
 * @author xBlackCat
 */
public interface ISubSettings {
    int getIntVal();

    @Optional
    String getValue();
}
