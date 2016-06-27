package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.util.IValueGetter;

import java.io.IOException;

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
    protected IValueGetter loadProperties() throws IOException {
        return null;
    }
}
