package org.xblackcat.sjpu.settings;

/**
 * Implementation of the interface loads values into given interface
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
    <T> T get(Class<T> clazz) throws SettingsException;

    /**
     * Loads settings for specified interface. Gets prefix for value names from {@linkplain org.xblackcat.sjpu.settings.ann.Prefix @Prefix} annotation if any.
     *
     * @param clazz    target interface class for holding settings.
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if resource is missing.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    <T> T get(Class<T> clazz, boolean optional) throws SettingsException;

    /**
     * Loads settings for specified interface. Optionality of the settings is specified with {@linkplain org.xblackcat.sjpu.settings.ann.Optional @Optional} annotation.
     *
     * @param clazz      target interface class for holding settings.
     * @param prefixName override prefix for properties
     * @param <T>        target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not properly annotated
     */
    <T> T get(Class<T> clazz, String prefixName) throws SettingsException;

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
    <T> T get(Class<T> clazz, String prefixName, boolean optional) throws SettingsException;
}
