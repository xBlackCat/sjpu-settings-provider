package org.xblackcat.sjpu.settings;

import javassist.ClassClassPath;
import javassist.ClassPool;
import org.xblackcat.sjpu.settings.ann.SettingsSource;
import org.xblackcat.sjpu.settings.config.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

/**
 * 14.04.2014 15:22
 *
 * @author xBlackCat
 */
public final class Config {
    private final static PoolHolder POOL_HOLDER = new PoolHolder();

    private Config() {
    }

    /**
     * Initializes specified class with default values if any. A {@linkplain org.xblackcat.sjpu.settings.SettingsException} will be thrown
     * if the specified interface has methods without {@linkplain org.xblackcat.sjpu.settings.ann.DefaultValue} annotation
     */
    public static final AConfig Defaults = new DefaultConfig(POOL_HOLDER.pool);

    /**
     * Builds a config reader from .properties file specified by {@linkplain java.io.File File} object.
     *
     * @param file .properties file.
     * @return config reader
     */
    public static AConfig use(File file) {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }

        return new InputStreamConfig(POOL_HOLDER.pool, () -> InputStreamConfig.getInputStream(file));
    }

    /**
     * Builds a config reader from .properties file specified by {@linkplain Path Path} object.
     *
     * @param file .properties file.
     * @return config reader
     */
    public static AConfig use(Path file) {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }

        return new InputStreamConfig(POOL_HOLDER.pool, () -> InputStreamConfig.getInputStream(file));
    }

    /**
     * Builds a config reader from .properties file specified by url.
     *
     * @param url url to .properties file.
     * @return config reader
     */
    public static AConfig use(URL url) {
        if (url == null) {
            throw new NullPointerException("Url should be set");
        }

        return new InputStreamConfig(POOL_HOLDER.pool, url::openStream);
    }

    /**
     * Builds a config reader from .properties file located in class path resources.
     *
     * @param resourceName resource name.
     * @return config reader
     */
    public static AConfig use(String resourceName) {
        if (resourceName == null) {
            throw new NullPointerException("Resource name should be set");
        }

        return new InputStreamConfig(POOL_HOLDER.pool, () -> InputStreamConfig.buildInputStreamProvider(resourceName));
    }

    public static AConfig useEnv() {
        return new EnvConfig(Config.POOL_HOLDER.pool);
    }

    public static AConfig useJvm() {
        return new JvmConfig(Config.POOL_HOLDER.pool);
    }

    public static AConfig anyOf(AConfig... sources) {
        return new MultiSourceConfig(POOL_HOLDER.pool, sources);
    }

    /**
     * Builds a config reader from .properties file which location is specified by annotations in the given class.
     *
     * @param clazz class annotated with {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource} annotation.
     * @return config reader
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or interface is not annotated with
     *                                                       {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource}
     */
    public static AConfig use(Class<?> clazz) throws SettingsException {
        final SettingsSource sourceAnn = clazz.getAnnotation(SettingsSource.class);

        if (sourceAnn == null) {
            throw new SettingsException(
                    "No default source is specified for " + clazz.getName() + ". Should be specified with @" + SettingsSource.class +
                            " annotation"
            );
        }

        return use(sourceAnn.value());
    }

    /**
     * Loads settings for specified interface. A default location of resource file is used. Default location is specified
     * by {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource} annotation.
     * <p>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.ann.Optional} annotation a <code>null</code> value will be
     * returned in case when required resource is not exists.
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or interface is not annotated with
     *                                                       {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource}
     */
    public static <T> T get(Class<T> clazz) throws SettingsException {
        return use(clazz).get(clazz);
    }

    private static final class PoolHolder {
        private final ClassPool pool;
        private final ClassLoader classLoader = new ClassLoader(Config.class.getClassLoader()) {
        };

        private PoolHolder() {
            pool = new ClassPool(true) {
                @Override
                public ClassLoader getClassLoader() {
                    return classLoader;
                }
            };
            pool.appendClassPath(new ClassClassPath(AConfig.class));
        }
    }
}
