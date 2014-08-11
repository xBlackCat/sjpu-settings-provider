package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.settings.IConfig;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.Prefix;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 14.04.2014 14:43
 *
 * @author xBlackCat
 */
public abstract class AConfig implements IConfig {
    protected final Log log = LogFactory.getLog(getClass());

    private Map<String, String> loadedProperties;
    private final ClassPool pool;

    protected AConfig(ClassPool pool) {
        this.pool = pool;
    }

    static <T> String getPrefix(Class<T> clazz) {
        final String prefixName;
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        if (prefixAnn != null) {
            prefixName = prefixAnn.value();
        } else {
            prefixName = "";
        }
        return prefixName;
    }

    @Override
    public <T> T get(Class<T> clazz, boolean optional) throws SettingsException {
        return get(clazz, getPrefix(clazz), optional);
    }

    @Override
    public <T> T get(Class<T> clazz) throws SettingsException {
        return get(clazz, getPrefix(clazz));
    }

    @Override
    public <T> T get(Class<T> clazz, String prefixName) throws SettingsException {
        return get(clazz, prefixName, clazz.isAnnotationPresent(Optional.class));
    }

    @Override
    public <T> T get(Class<T> clazz, String prefixName, boolean optional) throws SettingsException {
        if (log.isDebugEnabled()) {
            log.debug("Load defaults for class " + clazz.getName());
        }

        ClassPool pool = ClassUtils.getClassPool(this.pool, clazz);

        @SuppressWarnings("unchecked") final Constructor<T> c = ClassUtils.getSettingsConstructor(clazz, pool);

        if (loadedProperties == null) {
            try {
                loadedProperties = loadProperties();
            } catch (IOException e) {
                throw new SettingsException("Can't obtain list of values for class " + clazz, e);
            }

            if (loadedProperties == null) {
                // Values are not loaded
                if (ClassUtils.allMethodsHaveDefaults(clazz)) {
                    loadedProperties = Collections.emptyMap(); // Avoid NPE
                } else if (optional) {
                    if (log.isTraceEnabled()) {
                        log.trace(clazz.getName() + " marked as optional");
                    }

                    // Optional means no exceptions - just return null
                    return null;
                } else {
                    throw new SettingsException(clazz.getName() + " has mandatory properties without default values");
                }
            }
        }

        List<Object> values = ClassUtils.buildConstructorParameters(pool, clazz, loadedProperties, prefixName);

        return ClassUtils.initialize(c, values);
    }

    protected abstract Map<String, String> loadProperties() throws IOException, SettingsException;
}
