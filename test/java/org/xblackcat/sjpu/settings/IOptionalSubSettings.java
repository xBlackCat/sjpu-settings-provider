package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.PropertyName;

/**
 * 08.12.2015 12:00
 *
 * @author xBlackCat
 */
public interface IOptionalSubSettings {
    @Optional
    @PropertyName("subbb")
    ISubSettings getSubSettings();

    String getName();
}
