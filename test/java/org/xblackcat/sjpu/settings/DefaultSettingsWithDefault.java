package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.DefaultValue;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.SettingsSource;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@SettingsSource("/source/settings")
public interface DefaultSettingsWithDefault {
    default boolean alwaysTrue() {
        return true;
    }

    @DefaultValue("100")
    int getSimpleName();

    @DefaultValue("0")
    int getComplexNameWithABBR();

    /**
     * Test reserved words filed
     */
    @Optional
    String getInterface();
}
