package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.DefaultValue;
import org.xblackcat.sjpu.settings.ann.Description;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.SettingsSource;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@SettingsSource("/source/settings")
public interface SettingsWithDefault {
    default boolean alwaysTrue() {
        return true;
    }

    @Optional
    @Description("Optional integer value")
    Integer getSimpleName();

    @DefaultValue("0")
    int getComplexNameWithABBR();
}
