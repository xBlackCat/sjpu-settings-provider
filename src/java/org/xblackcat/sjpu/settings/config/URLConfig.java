package org.xblackcat.sjpu.settings.config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * 14.04.2014 15:07
 *
 * @author xBlackCat
 */
public class URLConfig extends AnInputStreamConfig {
    private final URL url;

    public URLConfig(URL url) {
        this.url = url;
    }

    @Override
    protected Map<String, String> loadProperties() throws IOException {
        if (url == null) {
            return null;
        }

        try (InputStream is = new BufferedInputStream(url.openStream())) {
            return loadPropertiesFromStream(is);
        }
    }
}
