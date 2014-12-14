package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 14.04.2014 15:07
 *
 * @author xBlackCat
 */
public class URLConfig extends AnInputStreamConfig {
    private final URL url;

    public URLConfig(ClassPool pool, URL url) {
        super(pool);
        this.url = url;
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        if (url == null) {
            return null;
        }

        try (InputStream is = new BufferedInputStream(url.openStream())) {
            return loadPropertiesFromStream(is);
        }
    }
}
