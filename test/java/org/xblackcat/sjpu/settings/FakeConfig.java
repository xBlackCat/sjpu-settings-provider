package org.xblackcat.sjpu.settings;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.config.AConfig;
import org.xblackcat.sjpu.settings.config.IValueGetter;
import org.xblackcat.sjpu.settings.config.MapWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 14.04.2014 17:07
 *
 * @author xBlackCat
 */
class FakeConfig extends AConfig {
    FakeConfig(ClassPool pool) {
        super(pool);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException, SettingsException {
        final Map<String, String> properties = new HashMap<>();
        properties.put("not.annotated", "true");
        properties.put("wrong.annotated", "true");
        return new MapWrapper(properties);
    }
}
