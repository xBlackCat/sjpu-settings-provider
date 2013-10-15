package org.xblackcat.sjpu.settings;

/**
 * 15.10.13 16:28
 *
 * @author xBlackCat
 */
@SettingsSource("/source/complex.properties")
public interface ComplexSettings {
    int[] getIds();

    boolean[] getFlags();

    Numbers[] getValues();
}
