package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.APrefixHandler;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.settings.util.LoadUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 14.04.2014 17:07
 *
 * @author xBlackCat
 */
public class FakeConfig extends APermanentConfig {
    public FakeConfig(
            ClassPool pool,
            Map<String, APrefixHandler> prefixHandlers,
            List<IValueGetter> substitutions
    ) {
        super(pool, prefixHandlers, substitutions);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        final Map<String, String> properties = new HashMap<>();
        properties.put("not.annotated", "true");
        properties.put("wrong.annotated", "true");
        return LoadUtils.wrap(properties);
    }
}
