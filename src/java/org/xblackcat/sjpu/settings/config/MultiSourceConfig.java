package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 12.12.2014 19:01
 *
 * @author xBlackCat
 */
public class MultiSourceConfig extends AConfig {
    private final AConfig[] sources;

    public MultiSourceConfig(ClassPool pool, AConfig... sources) {
        super(pool);
        if (ArrayUtils.isEmpty(sources)) {
            throw new IllegalArgumentException("Please, specify at least one source");
        }
        this.sources = sources;
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        final IValueGetter[] loadedProperties = new IValueGetter[sources.length];
        int i = 0;
        while (i < sources.length) {
            loadedProperties[i] = sources[i].loadProperties();
            i++;
        }

        return new MultiSourceValueGetter(loadedProperties);
    }

    private static class MultiSourceValueGetter implements IValueGetter {
        private final IValueGetter[] loadedProperties;
        private final Set<String> keySet = new HashSet<>();

        public MultiSourceValueGetter(IValueGetter[] loadedProperties) {
            this.loadedProperties = loadedProperties;
        }

        @Override
        public String get(String key) {
            for (IValueGetter getter : loadedProperties) {
                if (getter != null) {
                    String value = getter.get(key);
                    if (value != null) {
                        return value;
                    }
                }
            }
            return null;
        }

        @Override
        public Set<String> keySet() {
            if (keySet.isEmpty()) {
                for (IValueGetter getter : loadedProperties) {
                    keySet.addAll(getter.keySet());
                }
            }
            return keySet;
        }
    }
}
