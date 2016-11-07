package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.lang3.ArrayUtils;
import org.xblackcat.sjpu.settings.util.IValueGetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        final List<IValueGetter> loadedProperties = new ArrayList<>(sources.length);
        for (AConfig source : sources) {
            if (source != null) {
                final IValueGetter valueGetter = source.loadProperties();
                if (valueGetter != null) {
                    loadedProperties.add(valueGetter);
                }
            }
        }

        return new MultiSourceValueGetter(loadedProperties);
    }

    private static class MultiSourceValueGetter implements IValueGetter {
        private final List<IValueGetter> loadedProperties;
        private final Set<String> keySet = new HashSet<>();

        public MultiSourceValueGetter(List<IValueGetter> loadedProperties) {
            this.loadedProperties = loadedProperties;
        }

        @Override
        public String get(String key) {
            for (IValueGetter getter : loadedProperties) {
                String value = getter.get(key);
                if (value != null) {
                    return value;
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
