package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;

import java.io.IOException;
import java.util.Map;

/**
 * 14.04.2014 15:04
 *
 * @author xBlackCat
 */
public class DefaultConfig extends AConfig {
    public DefaultConfig(ClassPool pool) {
        super(pool);
    }

    @Override
    protected Map<String, String> loadProperties() throws IOException {
        return null;
    }
}
