package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.util.ClassUtils;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * 14.04.2014 14:43
 *
 * @author xBlackCat
 */
public abstract class APermanentConfig extends AConfig implements IConfig {
    private IValueGetter loadedProperties;

    public APermanentConfig(
            ClassPool pool,
            Map<String, UnaryOperator<String>> prefixHandlers,
            List<SupplierEx<IValueGetter, SettingsException>> substitutions
    ) {
        super(pool, prefixHandlers, substitutions);
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
    @Override
    public <T> T get(Class<T> clazz, String prefixName, boolean optional) throws SettingsException {
        if (log.isDebugEnabled()) {
            log.debug("Load defaults for class " + clazz.getName() + " [prefix: " + prefixName + "]");
        }

        ClassPool pool = BuilderUtils.getClassPool(this.pool, clazz);

        @SuppressWarnings("unchecked") final Constructor<T> c = ClassUtils.getSettingsConstructor(clazz, pool);

        IValueGetter loadedProperties = getValueGetter();
        
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

        List<Object> values = buildConstructorParameters(pool, clazz, prefixName, loadedProperties);

        return ClassUtils.initialize(c, values);
    }

    @Override
    public IValueGetter getValueGetter() throws SettingsException {
        if (loadedProperties == null) {
            try {
                loadedProperties = loadProperties();
            } catch (IOException e) {
                throw new SettingsException("Can't obtain list of values ", e);
            }

        }
        return loadedProperties;
    }

    protected abstract IValueGetter loadProperties() throws IOException;
}
