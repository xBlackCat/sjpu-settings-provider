package org.xblackcat.sjpu.settings.config;

import org.xblackcat.sjpu.settings.SettingsException;

/**
 * 08.11.2016 11:22
 *
 * @author xBlackCat
 */
public interface IConfig {
    <T> T get(Class<T> clazz) throws SettingsException;

    <T> T get(Class<T> clazz, boolean optional) throws SettingsException;

    <T> T get(Class<T> clazz, String prefixName) throws SettingsException;

    <T> T get(Class<T> clazz, String prefixName, boolean optional) throws SettingsException;
}
