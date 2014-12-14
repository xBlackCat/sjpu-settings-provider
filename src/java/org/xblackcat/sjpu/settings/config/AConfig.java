package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.Prefix;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * 14.04.2014 14:43
 *
 * @author xBlackCat
 */
public abstract class AConfig {
    protected final Log log = LogFactory.getLog(getClass());

    private IValueGetter loadedProperties;
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

    /**
     * Loads settings for specified interface. Gets prefix for value names from {@linkplain org.xblackcat.sjpu.settings.ann.Prefix @Prefix} annotation if any.
     *
     * @param clazz    target interface class for holding settings.
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if resource is missing.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    public <T> T get(Class<T> clazz, boolean optional) throws SettingsException {
        return get(clazz, getPrefix(clazz), optional);
    }

    /**
     * Loads settings for specified interface. Gets prefix for value names from {@linkplain org.xblackcat.sjpu.settings.ann.Prefix @Prefix}
     * annotation if any. Optionality of the settings is specified with {@linkplain org.xblackcat.sjpu.settings.ann.Optional @Optional} annotation.
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    public <T> T get(Class<T> clazz) throws SettingsException {
        return get(clazz, getPrefix(clazz));
    }

    /**
     * Loads settings for specified interface. Optionality of the settings is specified with {@linkplain org.xblackcat.sjpu.settings.ann.Optional @Optional} annotation.
     *
     * @param clazz      target interface class for holding settings.
     * @param prefixName override prefix for properties
     * @param <T>        target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    public <T> T get(Class<T> clazz, String prefixName) throws SettingsException {
        return get(clazz, prefixName, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface.
     *
     * @param clazz      target interface class for holding settings.
     * @param prefixName override prefix for properties
     * @param optional   <code>true</code> to return <code>null</code> instead of throwing exception if resource is missing.
     * @param <T>        target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
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
                    loadedProperties = IValueGetter.EMPTY; // Avoid NPE
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


    protected abstract IValueGetter loadProperties() throws IOException, SettingsException;
}
