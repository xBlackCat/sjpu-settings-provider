package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.util.IValueGetter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 14.04.2014 17:07
 *
 * @author xBlackCat
 */
public class FakeConfig extends AConfig {
    public FakeConfig(ClassPool pool) {
        super(pool);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        final Map<String, String> properties = new HashMap<>();
        properties.put("not.annotated", "true");
        properties.put("wrong.annotated", "true");
        return new MapWrapper(properties);
    }
}
