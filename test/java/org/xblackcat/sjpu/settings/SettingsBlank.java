package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.Optional;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@Optional
public interface SettingsBlank {
    int getSimpleName();

    int getComplexNameWithABBR();
}
