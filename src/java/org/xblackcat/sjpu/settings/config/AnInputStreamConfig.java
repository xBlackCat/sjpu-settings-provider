package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 14.04.2014 15:01
 *
 * @author xBlackCat
 */
public abstract class AnInputStreamConfig extends AConfig {
    public AnInputStreamConfig(ClassPool pool) {
        super(pool);
    }

    protected final Map<String, String> loadPropertiesFromStream(InputStream is) throws IOException {
        final Map<String, String> shadow = new LinkedHashMap<>();
        // Use properties object for loading values only
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final Properties properties = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                return shadow.put(ObjectUtils.toString(key, null), ObjectUtils.toString(value, null));
            }
        };
        properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));

        return shadow;
    }
}
