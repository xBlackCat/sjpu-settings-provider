package org.xblackcat.sjpu.settings.config;

import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.Prefix;

/**
 * 08.11.2016 11:22
 *
 * @author xBlackCat
 */
public interface IConfig {
    /**
     * Loads settings for specified interface. Gets prefix for value names from {@linkplain org.xblackcat.sjpu.settings.ann.Prefix @Prefix}
     * annotation if any. Optionality of the settings is specified with {@linkplain org.xblackcat.sjpu.settings.ann.Optional @Optional} annotation.
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    default <T> T get(Class<T> clazz) throws SettingsException {
        return get(clazz, clazz.isAnnotationPresent(Optional.class));
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
    default <T> T get(Class<T> clazz, boolean optional) throws SettingsException {
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        return get(clazz, prefixAnn != null ? prefixAnn.value() : "", optional);
    }

    /**
     * Loads settings for specified interface. Optionality of the settings is specified with {@linkplain Optional @Optional} annotation.
     *
     * @param clazz      target interface class for holding settings.
     * @param prefixName override prefix for properties
     * @param <T>        target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    default <T> T get(Class<T> clazz, String prefixName) throws SettingsException {
        return get(clazz, prefixName, clazz.isAnnotationPresent(Optional.class));
    }

    <T> T get(Class<T> clazz, String prefixName, boolean optional) throws SettingsException;
}
