package org.xblackcat.sjpu.settings.config;

/**
 * 07.11.2016 9:34
 *
 * @author xBlackCat
 */
public interface ISettingsWrapper<T> {
    void setConfig(T config);

    T getConfig();
}
