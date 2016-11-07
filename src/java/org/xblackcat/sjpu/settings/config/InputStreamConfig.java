package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.settings.util.LoadUtils;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.IOException;
import java.io.InputStream;

/**
 * 14.04.2014 15:01
 *
 * @author xBlackCat
 */
public class InputStreamConfig extends AConfig {
    private final SupplierEx<InputStream, IOException> inputStreamProvider;

    public InputStreamConfig(ClassPool pool, SupplierEx<InputStream, IOException> inputStreamProvider) {
        super(pool);
        this.inputStreamProvider = inputStreamProvider;
    }

    @Override
    protected final IValueGetter loadProperties() throws IOException {
        return LoadUtils.loadProperties(inputStreamProvider);
    }
}
