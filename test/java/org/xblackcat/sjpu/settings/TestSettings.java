package org.xblackcat.sjpu.settings;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
public interface TestSettings {
    @SettingsField
    int getSimpleName();

    @SettingsField
    int getComplexNameWithABBR();
}
