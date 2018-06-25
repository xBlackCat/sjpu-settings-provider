package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * 14.04.2014 15:04
 *
 * @author xBlackCat
 */
public class DefaultConfig extends APermanentConfig {
    public DefaultConfig(
            ClassPool pool,
            Map<String, UnaryOperator<String>> prefixHandlers,
            List<SupplierEx<IValueGetter, SettingsException>> substitutions
    ) {
        super(pool, prefixHandlers, substitutions);
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        return null;
    }
}
