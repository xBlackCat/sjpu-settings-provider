package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.lang3.ArrayUtils;
import org.xblackcat.sjpu.settings.APrefixHandler;
import org.xblackcat.sjpu.settings.util.IValueGetter;

import java.io.IOException;
import java.util.*;

/**
 * 12.12.2014 19:01
 *
 * @author xBlackCat
 */
public class MultiSourceConfig extends APermanentConfig {
    private final APermanentConfig[] sources;

    public MultiSourceConfig(
            ClassPool pool,
            Map<String, APrefixHandler> prefixHandlers,
            List<IValueGetter> substitutions,
            IConfig... sources
    ) {
        super(pool, prefixHandlers, substitutions);
        if (ArrayUtils.isEmpty(sources)) {
            throw new IllegalArgumentException("Please, specify at least one source");
        }
        this.sources = Arrays.stream(sources)
                .filter(c -> c instanceof APermanentConfig)
                .map(c -> (APermanentConfig) c)
                .toArray(APermanentConfig[]::new);

        if (ArrayUtils.isEmpty(this.sources)) {
            throw new IllegalArgumentException("Please, specify at least one source: Mutable config is not supported");
        }
    }

    @Override
    protected IValueGetter loadProperties() throws IOException {
        final List<IValueGetter> loadedProperties = new ArrayList<>(sources.length);
        for (APermanentConfig source: sources) {
            final IValueGetter valueGetter = source.loadProperties();
            if (valueGetter != null) {
                loadedProperties.add(valueGetter);
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
            for (IValueGetter getter: loadedProperties) {
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
                for (IValueGetter getter: loadedProperties) {
                    keySet.addAll(getter.keySet());
                }
            }
            return keySet;
        }
    }
}
