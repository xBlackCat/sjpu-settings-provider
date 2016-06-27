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
public interface Settings {
    @Optional
    String getSimpleName();

    @DefaultValue("0")
    int getComplexNameWithABBR();
}
