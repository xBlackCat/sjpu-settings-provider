package org.xblackcat.sjpu.settings;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@SettingsSource("/source/settings")
public interface TestSettings {
    @SettingsField(required = false)
    int getSimpleName();

    @SettingsField(defaultValue = "0")
    int getComplexNameWithABBR();
}
