package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.SettingsException;

import java.io.IOException;
import java.util.Set;

/**
 * 12.12.2014 18:48
 *
 * @author xBlackCat
 */
public class JvmConfig extends AConfig {
    public JvmConfig(ClassPool pool) {
        super(pool);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException, SettingsException {
        return new IValueGetter() {
            @Override
            public String get(String key) {
                return System.getProperties().getProperty(key);
            }

            @Override
            public Set<String> keySet() {
                return System.getProperties().stringPropertyNames();
            }
        };
    }
}
