package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.SettingsSource;
import org.xblackcat.sjpu.settings.config.DefaultConfig;
import org.xblackcat.sjpu.settings.config.FileConfig;
import org.xblackcat.sjpu.settings.config.ResourceConfig;
import org.xblackcat.sjpu.settings.config.URLConfig;

import java.io.File;
import java.net.URL;

/**
 * 14.04.2014 15:22
 *
 * @author xBlackCat
 */
public final class Config {
    private Config() {
    }

    /**
     * Initializes specified class with default values if any. A {@linkplain org.xblackcat.sjpu.settings.SettingsException} will be thrown
     * if the specified interface has methods without {@linkplain org.xblackcat.sjpu.settings.ann.DefaultValue} annotation
     */
    public static final IConfig Defaults = new DefaultConfig();

    /**
     * Builds a config reader from .properties file specified by {@linkplain java.io.File File} object.
     *
     * @param file .properties file.
     * @return config reader
     */
    public static IConfig use(File file) {
        return new FileConfig(file);
    }

    /**
     * Builds a config reader from .properties file specified by url.
     *
     * @param url url to .properties file.
     * @return config reader
     */
    public static IConfig use(URL url) {
        return new URLConfig(url);
    }

    /**
     * Builds a config reader from .properties file located in class path resources.
     *
     * @param resourceName resource name.
     * @return config reader
     */
    public static IConfig use(String resourceName) {
        return new ResourceConfig(resourceName);
    }

    /**
     * Builds a config reader from .properties file which location is specified by annotations in the given class.
     *
     * @param clazz class annotated with {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource} annotation.
     * @return config reader
     */
    public static IConfig use(Class<?> clazz) throws SettingsException {
        final SettingsSource sourceAnn = clazz.getAnnotation(SettingsSource.class);

        if (sourceAnn == null) {
            throw new SettingsException(
                    "No default source is specified for " + clazz.getName() + ". Should be specified with @" + SettingsSource.class +
                            " annotation"
            );
        }

        return new ResourceConfig(sourceAnn.value());
    }

    /**
     * Loads settings for specified interface. A default location of resource file is used. Default location is specified
     * by {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource} annotation.
     * <p/>
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
}
