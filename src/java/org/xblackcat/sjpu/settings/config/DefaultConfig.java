package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.APrefixHandler;
import org.xblackcat.sjpu.settings.util.IValueGetter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 14.04.2014 15:04
 *
 * @author xBlackCat
 */
public class DefaultConfig extends APermanentConfig {
    public DefaultConfig(
            ClassPool pool,
            Map<String, APrefixHandler> prefixHandlers,
            List<IValueGetter> substitutions
    ) {
        super(pool, prefixHandlers, substitutions);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        return null;
    }
}
