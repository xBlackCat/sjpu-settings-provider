package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.settings.util.LoadUtils;

import java.io.IOException;

/**
 * 12.12.2014 18:48
 *
 * @author xBlackCat
 */
public class EnvConfig extends APermanentConfig {
    public EnvConfig(ClassPool pool) {
        super(pool);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        return LoadUtils.wrap(System.getenv());
    }
}
