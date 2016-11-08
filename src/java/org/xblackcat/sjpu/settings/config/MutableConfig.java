package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.SettingsException;
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
public class MutableConfig implements IMutableConfig, IConfig {
    private static final Log log = LogFactory.getLog(MutableConfig.class);

    protected final ClassPool pool;
    private final Lock lock = new ReentrantLock();
    private final List<IConfigListener> listenerList = new ArrayList<>();
    private final Map<ConfigInfo<?>, ISettingsWrapper<?>> loadedObjects = new HashMap<>();
    private final Path file;
    private final Path parent;
    private final APermanentConfig wrappedConfig;
    private final Consumer<Runnable> notifyConsumer;

    private volatile IValueGetter loadedProperties;

    public MutableConfig(ClassPool pool, Path file, Consumer<Runnable> notifyConsumer) {
        this.pool = pool;
        this.file = file;
        parent = file.getParent();

        this.notifyConsumer = notifyConsumer;
        wrappedConfig = new InputStreamConfig(pool, () -> LoadUtils.getInputStream(file));
    }

    @Override
    public <T> T get(Class<T> clazz, String prefixName, boolean optional) throws SettingsException {
        if (log.isDebugEnabled()) {
            log.debug("Initialize mutable config for class " + clazz.getName() + " [prefix: " + prefixName + "]");
        }

        ClassPool pool = BuilderUtils.getClassPool(this.pool, clazz);

        ConfigInfo<T> configInfo = new ConfigInfo<>(clazz, prefixName, optional);
        lock.lock();
        try {
            @SuppressWarnings("unchecked")
            T wrapper = (T) loadedObjects.get(configInfo);
            if (wrapper != null) {
                return wrapper;
            }

            IValueGetter properties;
            if (loadedProperties == null) {
                properties = reloadFile();
                loadedProperties = properties;
            } else {
                properties = loadedProperties;
            }

            T object = initObject(pool, configInfo, properties);

            Constructor<ISettingsWrapper<T>> dataWrapper = ClassUtils.getSettingsWrapperConstructor(clazz, pool);

            final ISettingsWrapper<T> dw = ClassUtils.initialize(dataWrapper, object);
            loadedObjects.put(configInfo, dw);

            // Wrapper also implements interface clazz
            @SuppressWarnings("unchecked")
            final T wrappedData = (T) dw;
            return wrappedData;
        } finally {
            lock.unlock();
        }
    }

    private <T> T initObject(ClassPool pool, ConfigInfo<T> configInfo, IValueGetter loadedProperties) throws SettingsException {
        Class<T> clazz = configInfo.getClazz();
        String prefixName = configInfo.getPrefix();
        boolean optional = configInfo.isOptional();

        try {
            @SuppressWarnings("unchecked") final Constructor<T> c = ClassUtils.getSettingsConstructor(clazz, pool);
            List<Object> values = ClassUtils.buildConstructorParameters(pool, clazz, prefixName, loadedProperties);

            return ClassUtils.initialize(c, values);
        } catch (SettingsException e) {
            if (optional) {
                return null;
            }
            throw e;
        }
    }

    private void reloadConfigs() {
        IValueGetter properties = reloadFile();

        lock.lock();
        try {
            loadedProperties = properties;

            for (Map.Entry<ConfigInfo<?>, ISettingsWrapper<?>> e : loadedObjects.entrySet()) {
                try {
                    ConfigInfo<?> configInfo = e.getKey();
                    ISettingsWrapper wrapper = e.getValue();
                    Class<?> clazz = configInfo.getClazz();

                    final Object data = initObject(pool, configInfo, properties);

                    Object oldData = wrapper.getConfig();
                    if (Objects.equals(data, oldData)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Configuration is not changed for " + configInfo);
                        }

                        continue;
                    }
                    updateObject(wrapper, data);

                    for (IConfigListener l : listenerList) {
                        notifyConsumer.accept(() -> l.onConfigChanged(clazz, configInfo.getPrefix(), data));
                    }
                } catch (Throwable ex) {
                    log.debug("Failed to parse properties - ignore request", ex);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private IValueGetter reloadFile() {
        IValueGetter properties = null;
        try {
            properties = wrappedConfig.loadProperties();
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to load properties - try to use defaults", e);
            }
        }

        if (properties == null) {
            if (log.isDebugEnabled()) {
                log.debug("No data is loaded - try to use defaults");
            }
            properties = IValueGetter.EMPTY;
        }
        return properties;
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
