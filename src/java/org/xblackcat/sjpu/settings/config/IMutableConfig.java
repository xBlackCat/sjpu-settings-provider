package org.xblackcat.sjpu.settings.config;

/**
 * 07.11.2016 15:53
 *
 * @author xBlackCat
 */
public interface IMutableConfig extends IConfig {
    void addListener(IConfigListener listener);

    void removeListener(IConfigListener listener);
}
