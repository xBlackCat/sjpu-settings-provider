package org.xblackcat.sjpu.settings;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@Prefix("super")
@SettingsSource("/source/settings-prefix.properties")
public interface SettingsPrefix {
    int getSimpleName();

    int getComplexNameWithABBR();
}
