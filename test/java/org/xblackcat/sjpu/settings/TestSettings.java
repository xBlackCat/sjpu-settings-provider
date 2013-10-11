package org.xblackcat.sjpu.settings;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@SettingsSource("/source/settings")
public interface TestSettings {
    @Optional
    int getSimpleName();

    @DefaultValue("0")
    int getComplexNameWithABBR();
}
