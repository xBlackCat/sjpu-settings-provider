package org.xblackcat.sjpu.settings;

import javassist.ClassPool;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 10.01.13 11:37
 *
 * @author xBlackCat
 */
public final class SettingsProvider {
    private static final Log log = LogFactory.getLog(SettingsProvider.class);

    /**
     * Loads settings for specified interface from specified file.
     * <p/>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.Optional} annotation a <code>null</code> value will be
     * returned in case when specified file is <code>null</code> or not exists.
     *
     * @param clazz target interface class for holding settings.
     * @param file  source input file (properties file)
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, File file) throws SettingsException, IOException {
        return get(clazz, file, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface from specified file.
     *
     * @param clazz    target interface class for holding settings.
     * @param file     source input file (properties file)
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if file is null or not exists.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, File file, boolean optional) throws SettingsException, IOException {
        if (file != null && file.canRead()) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                return get(clazz, is, optional);
            }
        } else {
            return get(clazz, (InputStream) null, optional);
        }
    }

    /**
     * Loads settings for specified interface from specified resource specified by URI.
     * <p/>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.Optional} annotation a <code>null</code> value will be
     * returned in case when specified uri is <code>null</code>.
     *
     * @param clazz target interface class for holding settings.
     * @param uri   Uri to .properties file
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, URI uri) throws SettingsException, IOException {
        return get(clazz, uri, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface from specified resource specified by URI.
     *
     * @param clazz    target interface class for holding settings.
     * @param uri      Uri to .properties file
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if uri is <code>null</code>.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, URI uri, boolean optional) throws SettingsException, IOException {
        return get(clazz, uri == null ? null : uri.toURL(), optional);
    }

    /**
     * Loads settings for specified interface from specified resource specified by URL.
     * <p/>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.Optional} annotation a <code>null</code> value will be
     * returned in case when specified url is <code>null</code>.
     *
     * @param clazz target interface class for holding settings.
     * @param url   Url to .properties file
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, URL url) throws SettingsException, IOException {
        return get(clazz, url, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface from specified resource specified by URL.
     *
     * @param clazz    target interface class for holding settings.
     * @param url      Url to .properties file
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if url is <code>null</code>.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, URL url, boolean optional) throws SettingsException, IOException {
        if (url != null) {
            try (InputStream is = new BufferedInputStream(url.openStream())) {
                return get(clazz, is, optional);
            }
        } else {
            return get(clazz, (InputStream) null, optional);
        }
    }

    /**
     * Loads settings for specified interface from specified InputStream. Input stream remains open after reading.
     * <p/>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.Optional} annotation a <code>null</code> value will be
     * returned in case when specified {@linkplain java.io.InputStream} is <code>null</code>.
     *
     * @param clazz target interface class for holding settings.
     * @param is    source input stream (properties file)
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, InputStream is) throws SettingsException, IOException {
        return get(clazz, is, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface from specified InputStream. Input stream remains open after reading.
     *
     * @param clazz    target interface class for holding settings.
     * @param is       source input stream (properties file)
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if resource stream is <code>null</code>.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, final InputStream is, boolean optional) throws SettingsException, IOException {
        final Map<String, String> shadow = new LinkedHashMap<>();
        if (is != null) {
            // Use properties object for loading values only
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
            final Properties properties = new Properties() {
                @Override
                public synchronized Object put(Object key, Object value) {
                    return shadow.put(ObjectUtils.toString(key, null), ObjectUtils.toString(value, null));
                }
            };
            properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } else if (!ClassUtils.allMethodsHaveDefaults(clazz)) {
            if (optional) {
                if (log.isTraceEnabled()) {
                    log.trace(clazz.getName() + " marked as optional");
                }

                // Optional means no exceptions - just return null
                return null;
            }

            throw new SettingsException(
                    "Input stream is null and " +
                            clazz.getName() +
                            " has properties not annotated with " +
                            DefaultValue.class.getName()
            );
        }

        return loadValues(clazz, shadow);
    }

    /**
     * Loads settings for specified interface. A default location of resource file is used. Default location is specified
     * by {@linkplain SettingsSource @SettingsSource} annotation.
     * <p/>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.Optional} annotation a <code>null</code> value will be
     * returned in case when required resource is not exists.
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or interface is not annotated with
     *                                                       {@linkplain SettingsSource @SettingsSource}
     */
    public static <T> T get(Class<T> clazz) throws SettingsException {
        return get(clazz, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface. A default location of resource file is used. Default location is specified
     * by {@linkplain org.xblackcat.sjpu.settings.SettingsSource @SettingsSource} annotation.
     *
     * @param clazz    target interface class for holding settings.
     * @param optional <code>true</code> to return <code>null</code> instead of throwing exception if resource is missing.
     * @param <T>      target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or interface is not annotated with
     *                           {@linkplain org.xblackcat.sjpu.settings.SettingsSource @SettingsSource}
     */
    public static <T> T get(Class<T> clazz, boolean optional) throws SettingsException {
        final SettingsSource sourceAnn = clazz.getAnnotation(SettingsSource.class);

        if (sourceAnn == null) {
            throw new SettingsException(
                    "No default source is specified for " +
                            clazz.getName() +
                            ". Should be annotated with @SettingsSource annotation"
            );
        }

        final String propertiesFile = sourceAnn.value();
        if (log.isDebugEnabled()) {
            log.debug("Load properties from " + propertiesFile);
        }

        return get(clazz, propertiesFile, optional);
    }

    /**
     * Loads settings for specified interface from specified resource in class path.
     * <p/>
     * If specified class marked with {@linkplain org.xblackcat.sjpu.settings.Optional} annotation a <code>null</code> value will be
     * returned in case when specified resource name is <code>null</code> or resource is not exists.
     *
     * @param clazz        target interface class for holding settings.
     * @param resourceName resource name (properties file)
     * @param <T>          target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, String resourceName) throws SettingsException {
        return get(clazz, resourceName, clazz.isAnnotationPresent(Optional.class));
    }

    /**
     * Loads settings for specified interface from specified resource in class path.
     *
     * @param clazz        target interface class for holding settings.
     * @param resourceName resource name (properties file)
     * @param optional     <code>true</code> to return <code>null</code> instead of throwing exception if resource is missing.
     * @param <T>          target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, String resourceName, boolean optional) throws SettingsException {
        InputStream is = null;
        try {
            if (!resourceName.endsWith(".properties")) {
                is = getInputStream(resourceName + ".properties");
            }

            if (is == null) {
                is = getInputStream(resourceName);
            }

            return get(clazz, is, optional);
        } catch (IOException e) {
            throw new SettingsException("Can't load values for " + clazz.getName(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("Can't close stream. [" + clazz.getName() + "]", e);
                }
            }
        }
    }

    /**
     * Initializes specified class with default values if any. A {@linkplain org.xblackcat.sjpu.settings.SettingsException} will be thrown
     * if the specified interface has methods without {@linkplain org.xblackcat.sjpu.settings.DefaultValue} annotation
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return instance of the implementation class of the specified interface initialized with default values.
     * @throws SettingsException
     */
    public <T> T getDefaults(Class<T> clazz) throws SettingsException {
        if (!ClassUtils.allMethodsHaveDefaults(clazz)) {
            throw new SettingsException(
                    "Input stream is null and " +
                            clazz.getName() +
                            " has properties not annotated with " +
                            DefaultValue.class.getName()
            );
        }

        return loadValues(clazz, Collections.<String, String>emptyMap());
    }

    static <T> T loadValues(Class<T> clazz, Map<String, String> properties) throws SettingsException {
        if (log.isDebugEnabled()) {
            log.debug("Load defaults for class " + clazz.getName());
        }

        final String prefixName;
        final ClassPool pool = ClassPool.getDefault();
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        if (prefixAnn != null) {
            prefixName = prefixAnn.value();

            if (log.isDebugEnabled()) {
                log.debug("Prefix for property names is '" + prefixName + "'");
            }
        } else {
            prefixName = "";
        }

        @SuppressWarnings("unchecked") final Constructor<T> c = ClassUtils.getSettingsConstructor(clazz, pool);

        List<Object> values = ClassUtils.buildConstructorParameters(pool, clazz, properties, prefixName);

        return ClassUtils.initialize(c, values);
    }

    private static InputStream getInputStream(String propertiesFile) throws IOException {
        InputStream is = SettingsProvider.class.getResourceAsStream(propertiesFile);
        if (is == null) {
            is = SettingsProvider.class.getClassLoader().getResourceAsStream(propertiesFile);
        }

        return is;
    }
}
