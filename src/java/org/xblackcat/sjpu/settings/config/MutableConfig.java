package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.Prefix;
import org.xblackcat.sjpu.settings.util.ClassUtils;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.settings.util.LoadUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 03.11.2016 15:03
 *
 * @author xBlackCat
 */
public class MutableConfig implements IMutableConfig {
    private static final Log log = LogFactory.getLog(MutableConfig.class);

    private final Lock lock = new ReentrantLock();
    private final List<IConfigListener> listenerList = new ArrayList<>();
    private final Map<ConfigInfo<?>, ISettingsWrapper<?>> loadedObjects = new HashMap<>();
    private final ClassPool pool;
    private final Path file;
    private final Path parent;
    private final AConfig wrappedConfig;
    private final Consumer<Runnable> notifyConsumer;

    private volatile IValueGetter loadedProperties;

    public MutableConfig(ClassPool pool, Consumer<Runnable> notifyConsumer, Path file) throws IOException {
        this.pool = pool;
        this.file = file;
        parent = file.getParent();

        this.notifyConsumer = notifyConsumer;
        wrappedConfig = new InputStreamConfig(pool, () -> LoadUtils.getInputStream(file));
    }

    @Override
    public <T> T get(Class<T> clazz) throws SettingsException {
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        return get(clazz, prefixAnn != null ? prefixAnn.value() : "");
    }


    @Override
    public <T> T get(Class<T> clazz, String prefixName) throws SettingsException {
        if (log.isDebugEnabled()) {
            log.debug("Initialize mutable config for class " + clazz.getName() + " [prefix: " + prefixName + "]");
        }

        ClassPool pool = BuilderUtils.getClassPool(this.pool, clazz);

        T data = loadDataObject(pool, clazz, prefixName);

        Constructor<ISettingsWrapper<T>> dataWrapper = ClassUtils.getSettingsWrapperConstructor(clazz, pool);

        ISettingsWrapper<T> dw = ClassUtils.initialize(dataWrapper, data);
        loadedObjects.put(new ConfigInfo<>(clazz, prefixName), dw);

        return (T) dw;
    }

    private <T> T loadDataObject(ClassPool pool, Class<T> clazz, String prefixName) throws SettingsException {
        lock.lock();
        try {
            @SuppressWarnings("unchecked") final Constructor<T> c = ClassUtils.getSettingsConstructor(clazz, pool);

            if (loadedProperties == null) {
                try {
                    loadedProperties = wrappedConfig.loadProperties();
                } catch (IOException e) {
                    throw new SettingsException("Can't obtain list of values for class " + clazz, e);
                }

                if (loadedProperties == null) {
                    // Values are not loaded
                    if (ClassUtils.allMethodsHaveDefaults(clazz)) {
                        loadedProperties = IValueGetter.EMPTY; // Avoid NPE
                    } else {
                        throw new SettingsException(clazz.getName() + " has mandatory properties without default values");
                    }
                }
            }

            IValueGetter loadedProperties = this.loadedProperties;
            List<Object> values = ClassUtils.buildConstructorParameters(pool, clazz, prefixName, loadedProperties);

            return ClassUtils.initialize(c, values);
        } finally {
            lock.unlock();
        }

    }

    private void reloadConfigs() {
        final IValueGetter properties;
        try {
            properties = wrappedConfig.loadProperties();
            if (properties == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No data is loaded - ignore reload event");
                }
                return;
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to load properties - ignore reload event", e);
            }
            return;
        }

        lock.lock();
        try {
            loadedProperties = properties;
        } finally {
            lock.unlock();
        }

        lock.lock();
        try {
            for (Map.Entry<ConfigInfo<?>, ISettingsWrapper<?>> e : loadedObjects.entrySet()) {
                try {
                    ConfigInfo<?> configInfo = e.getKey();
                    ISettingsWrapper wrapper = e.getValue();

                    final Constructor<?> c = ClassUtils.getSettingsConstructor(configInfo.getClazz(), pool);
                    List<Object> values = ClassUtils.buildConstructorParameters(
                            pool,
                            configInfo.getClazz(),
                            configInfo.getPrefix(),
                            loadedProperties
                    );

                    Object data = ClassUtils.initialize(c, values);

                    Object oldData = wrapper.getConfig();
                    if (Objects.equals(data, oldData)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Configuration is not changed for " + configInfo);
                        }

                        continue;
                    }
                    updateObject(wrapper, data);

                    for (IConfigListener l : listenerList) {
                        notifyConsumer.accept(() -> l.onConfigChanged(configInfo.getClazz(), configInfo.getPrefix(), data));
                    }
                } catch (Throwable ex) {
                    log.debug("Failed to parse properties - ignore request", ex);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private void updateObject(ISettingsWrapper wrapper, Object data) {
        wrapper.setConfig(data);
    }

    @Override
    public void addListener(IConfigListener listener) {
        lock.lock();
        try {
            listenerList.add(listener);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeListener(IConfigListener listener) {
        lock.lock();
        try {
            listenerList.remove(listener);
        } finally {
            lock.unlock();
        }
    }

    public void checkPaths(Set<Path> paths) {
        for (Path p : paths) {
            if (parent.resolve(p).equals(file)) {
                reloadConfigs();
                return;
            }
        }
    }
}
