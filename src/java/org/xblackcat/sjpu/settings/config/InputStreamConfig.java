package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.settings.util.LoadUtils;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * 14.04.2014 15:01
 *
 * @author xBlackCat
 */
public class InputStreamConfig extends APermanentConfig {
    private final SupplierEx<InputStream, IOException> inputStreamProvider;

    public InputStreamConfig(
            ClassPool pool,
            Map<String, UnaryOperator<String>> prefixHandlers,
            List<SupplierEx<IValueGetter, SettingsException>> substitutions,
            SupplierEx<InputStream, IOException> inputStreamProvider
    ) {
        super(pool, prefixHandlers, substitutions);
        this.inputStreamProvider = inputStreamProvider;
    }

    @Override
    protected final IValueGetter loadProperties() throws IOException {
        return LoadUtils.loadProperties(inputStreamProvider);
    }
}
