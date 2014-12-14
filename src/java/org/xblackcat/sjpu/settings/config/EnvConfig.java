package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.SettingsException;

import java.io.IOException;

/**
 * 12.12.2014 18:48
 *
 * @author xBlackCat
 */
public class EnvConfig extends AConfig {
    public EnvConfig(ClassPool pool) {
        super(pool);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException, SettingsException {
        return new MapWrapper(System.getenv());
    }
}
