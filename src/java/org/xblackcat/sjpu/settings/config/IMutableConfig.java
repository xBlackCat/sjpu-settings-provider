package org.xblackcat.sjpu.settings.config;

import org.xblackcat.sjpu.settings.SettingsException;

/**
 * 07.11.2016 15:53
 *
 * @author xBlackCat
 */
public interface IMutableConfig {
    <T> T get(Class<T> clazz) throws SettingsException;

    <T> T get(Class<T> clazz, String prefixName) throws SettingsException;

    void addListener(IConfigListener listener);

    void removeListener(IConfigListener listener);
}
