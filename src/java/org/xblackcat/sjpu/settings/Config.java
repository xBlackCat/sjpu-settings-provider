package org.xblackcat.sjpu.settings;

import javassist.ClassClassPath;
import javassist.ClassPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.settings.ann.SettingsSource;
import org.xblackcat.sjpu.settings.config.*;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.settings.util.LoadUtils;
import org.xblackcat.sjpu.settings.util.MapWrapper;
import org.xblackcat.sjpu.util.function.SupplierEx;
import org.xblackcat.sjpu.util.thread.CustomNameThreadFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;

/**
 * 14.04.2014 15:22
 *
 * @author xBlackCat
 */
public final class Config {
    private final static PoolHolder POOL_HOLDER = new PoolHolder();

    private static final SettingsWatchingDaemon WATCHING_DAEMON;
    private static final Executor notifyExecutor;
    private static final UnsupportedOperationException EXCEPTION;

    private static final IValueGetter JVM_VALUES_GETTER = new IValueGetter() {
        @Override
        public String get(String key) {
            return System.getProperty(key);
        }

        @Override
        public Set<String> keySet() {
            return System.getProperties().stringPropertyNames();
        }
    };
    private static final IValueGetter ENV_VALUES_GETTER = LoadUtils.wrap(System.getenv());

    static {
        SettingsWatchingDaemon daemon = null;
        UnsupportedOperationException ex = null;
        try {
            daemon = new SettingsWatchingDaemon();
            final Thread thread = new Thread(daemon, "Settings Watching Daemon");
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new IOError(e);
        } catch (UnsupportedOperationException e) {
            ex = e;
        }
        EXCEPTION = ex;
        WATCHING_DAEMON = daemon;
        notifyExecutor = new ThreadPoolExecutor(
                0,
                15,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new CustomNameThreadFactory("notify-thread", "SettingsNotifier")
        );
    }

    private static void postNotify(Runnable event) {
        notifyExecutor.execute(() -> {
            try {
                event.run();
            } catch (Throwable e) {
                SettingsWatchingDaemon.log.error("Failed to process notify", e);
            }
        });
    }

    private Config() {
    }

    /**
     * Initializes specified class with default values if any. A {@linkplain org.xblackcat.sjpu.settings.SettingsException} will be thrown
     * if the specified interface has methods without {@linkplain org.xblackcat.sjpu.settings.ann.DefaultValue} annotation
     */
    public static final IConfig Defaults = Builder.defaultSettings().defaults();

    /**
     * Builds a config reader from .properties file specified by {@linkplain java.io.File File} object.
     *
     * @param file .properties file.
     * @return config reader
     */
    public static IConfig use(File file) {
        return Builder.defaultSettings().use(file);
    }

    /**
     * Builds a config reader from .properties file specified by {@linkplain Path Path} object.
     *
     * @param file .properties file.
     * @return config reader
     */
    public static IConfig use(Path file) {
        return Builder.defaultSettings().use(file);
    }

    /**
     * Builds a config reader from .properties file specified by url.
     *
     * @param url url to .properties file.
     * @return config reader
     */
    public static IConfig use(URL url) {
        return Builder.defaultSettings().use(url);
    }

    /**
     * Builds a config reader from .properties file located in class path resources.
     *
     * @param resourceName resource name.
     * @return config reader
     */
    public static IConfig use(String resourceName) {
        return Builder.defaultSettings().use(resourceName);
    }

    /**
     * Builds a config reader from .properties file located in class path resources.
     *
     * @param inputStreamSupplier input stream provider with all the
     * @return config reader
     */
    public static IConfig use(SupplierEx<InputStream, IOException> inputStreamSupplier) {
        return Builder.defaultSettings().use(inputStreamSupplier);
    }

    public static IConfig useEnv() {
        return Builder.defaultSettings().useEnv();
    }

    public static IConfig useJvm() {
        return Builder.defaultSettings().useJvm();
    }

    public static IConfig anyOf(IConfig... sources) {
        return Builder.defaultSettings().anyOf(sources);
    }

    /**
     * Builds a config reader from .properties file which location is specified by annotations in the given class.
     *
     * @param clazz class annotated with {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource} annotation.
     * @return config reader
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or interface is not annotated with
     *                                                       {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource}
     */
    public static IConfig use(Class<?> clazz) throws SettingsException {
        return Builder.defaultSettings().use(extractSource(clazz));
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
        return Builder.defaultSettings().use(clazz).get(clazz);
    }

    public static IMutableConfig track(Class<?> clazz) throws SettingsException, IOException, UnsupportedOperationException {
        return Builder.defaultSettings().track(extractSource(clazz), clazz.getClassLoader());
    }

    public static IMutableConfig track(String resourceName) throws IOException, UnsupportedOperationException {
        return Builder.defaultSettings().track(resourceName, Config.class.getClassLoader());
    }

    public static IMutableConfig track(String resourceName, ClassLoader classLoader) throws IOException, UnsupportedOperationException {
        return Builder.defaultSettings().track(resourceName, classLoader);
    }

    public static IMutableConfig track(Path file) throws IOException, UnsupportedOperationException {
        return Builder.defaultSettings().track(file);
    }

    public static IMutableConfig track(File file) throws IOException, UnsupportedOperationException {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }
        return track(file.toPath());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder with(String prefix, UnaryOperator<String> valueHandler) {
        return builder().with(prefix, valueHandler);
    }

    public static Builder substitute(Map<String, String> substitution) {
        return builder().substitute(substitution);
    }

    public static Builder substituteEnv() {
        return builder().substituteEnv();
    }

    public static Builder substituteJvm() {
        return builder().substituteJvm();
    }

    public static Builder substitute(IConfig substitution) {
        return builder().substitute(substitution);
    }

    private static String extractSource(Class<?> clazz) throws SettingsException {
        final SettingsSource sourceAnn = clazz.getAnnotation(SettingsSource.class);

        if (sourceAnn == null) {
            throw new SettingsException(
                    "No default source is specified for " + clazz.getName() + ". Should be specified with @" + SettingsSource.class +
                            " annotation"
            );
        }

        return sourceAnn.value();
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
            pool.appendClassPath(new ClassClassPath(IConfig.class));
        }
    }

    private final static class SettingsWatchingDaemon implements Runnable {
        private static final Log log = LogFactory.getLog(SettingsWatchingDaemon.class);

        private final WatchService watchService = FileSystems.getDefault().newWatchService();
        private final Map<Path, MutableConfig> trackedFiles = new WeakHashMap<>();
        private final Map<WatchKey, List<MutableConfig>> trackers = new WeakHashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        private IMutableConfig watch(
                Path file,
                Map<String, UnaryOperator<String>> prefixHandler,
                List<SupplierEx<IValueGetter, SettingsException>> subtitution
        ) throws IOException {
            lock.writeLock().lock();
            try {
                MutableConfig config = trackedFiles.get(file);
                if (config != null) {
                    return config;
                }

                MutableConfig newConfig = new MutableConfig(POOL_HOLDER.pool, prefixHandler, subtitution, file, Config::postNotify);
                final WatchKey watchKey = file.getParent().register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE
                );

                trackers.computeIfAbsent(watchKey, k -> new ArrayList<>()).add(newConfig);
                trackedFiles.put(file, newConfig);
                return newConfig;
            } finally {
                lock.writeLock().unlock();
            }
        }

        private SettingsWatchingDaemon() throws IOException {
        }

        @Override
        public void run() {
            for (; ; ) {
                final WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    return;
                }

                if (!key.isValid()) {
                    lock.writeLock().lock();
                    try {
                        trackers.remove(key);
                    } finally {
                        lock.writeLock().unlock();
                    }

                    continue;
                }

                final List<MutableConfig> configs;
                lock.readLock().lock();
                try {
                    configs = trackers.get(key);
                } finally {
                    lock.readLock().unlock();
                }

                if (configs != null) {
                    Set<Path> paths = new HashSet<>();
                    for (WatchEvent<?> event: key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        Path filename = getContext(event);

                        paths.add(filename);
                    }

                    for (MutableConfig c: configs) {
                        c.checkPaths(paths);
                    }
                }
                key.reset();
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T getContext(WatchEvent<?> event) {
            return ((WatchEvent<T>) event).context();
        }
    }

    public static class Builder {
        private final Map<String, UnaryOperator<String>> prefixHandlers = new HashMap<>();
        private final List<SupplierEx<IValueGetter, SettingsException>> substitutions = new ArrayList<>();

        private static Builder defaultSettings() {
            Builder builder = new Builder();
            builder.substitutions.add(() -> JVM_VALUES_GETTER);
            builder.substitutions.add(() -> ENV_VALUES_GETTER);
            return builder;
        }

        /**
         * Register a value processor. If property value is started with prefix the prefix will be cut and remaining part will be passed to
         * valueHandler
         *
         * @param prefix
         * @param valueHandler
         * @return the config builder
         */
        public Builder with(String prefix, UnaryOperator<String> valueHandler) {
            prefixHandlers.put(prefix, valueHandler);
            return this;
        }

        public Builder substituteEnv() {
            substitutions.add(() -> ENV_VALUES_GETTER);
            return this;
        }

        public Builder substituteJvm() {
            substitutions.add(() -> JVM_VALUES_GETTER);
            return this;
        }

        public Builder substitute(Map<String, String> substitution) {
            if (substitution == null) {
                throw new NullPointerException("Substitution map is null");
            }
            if (!substitution.isEmpty()) {
                substitutions.add(() -> new MapWrapper(new HashMap<>(substitution)));
            }
            return this;
        }

        public Builder substitute(IConfig substitution) {
            if (!(substitution instanceof AConfig)) {
                throw new IllegalArgumentException("Unsupported config for substitution");
            }
            substitutions.add(((AConfig) substitution)::getValueGetter);
            return this;
        }

        /**
         * Builds a config reader from .properties file specified by {@linkplain java.io.File File} object.
         *
         * @param file .properties file.
         * @return config reader
         */
        public IConfig use(File file) {
            if (file == null) {
                throw new NullPointerException("File can't be null");
            }

            return use(() -> LoadUtils.getInputStream(file));
        }

        public IConfig defaults() {
            return new DefaultConfig(POOL_HOLDER.pool, prefixHandlers, substitutions);
        }

        /**
         * Builds a config reader from .properties file specified by {@linkplain Path Path} object.
         *
         * @param file .properties file.
         * @return config reader
         */
        public IConfig use(Path file) {
            if (file == null) {
                throw new NullPointerException("File can't be null");
            }

            return use(() -> LoadUtils.getInputStream(file));
        }

        /**
         * Builds a config reader from .properties file specified by url.
         *
         * @param url url to .properties file.
         * @return config reader
         */
        public IConfig use(URL url) {
            if (url == null) {
                throw new NullPointerException("Url should be set");
            }

            return use(url::openStream);
        }

        /**
         * Builds a config reader from .properties file located in class path resources.
         *
         * @param resourceName resource name.
         * @return config reader
         */
        public IConfig use(String resourceName) {
            return use(() -> LoadUtils.buildInputStreamProvider(resourceName));
        }

        /**
         * Builds a config reader from .properties file located in class path resources.
         *
         * @param inputStreamSupplier input stream provider with all the
         * @return config reader
         */
        public IConfig use(SupplierEx<InputStream, IOException> inputStreamSupplier) {
            return new InputStreamConfig(POOL_HOLDER.pool, prefixHandlers, substitutions, inputStreamSupplier);
        }

        public IConfig useEnv() {
            return new APermanentConfig(POOL_HOLDER.pool, prefixHandlers, substitutions) {
                @Override
                protected IValueGetter loadProperties() {
                    return ENV_VALUES_GETTER;
                }
            };
        }

        public IConfig useJvm() {
            return new APermanentConfig(POOL_HOLDER.pool, prefixHandlers, substitutions) {
                @Override
                protected IValueGetter loadProperties() {
                    return JVM_VALUES_GETTER;
                }
            };
        }

        public IConfig anyOf(IConfig... sources) {
            return new MultiSourceConfig(POOL_HOLDER.pool, prefixHandlers, substitutions, sources);
        }

        /**
         * Builds a config reader from .properties file which location is specified by annotations in the given class.
         *
         * @param clazz class annotated with {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource} annotation.
         * @return config reader
         * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or interface is not annotated with
         *                                                       {@linkplain org.xblackcat.sjpu.settings.ann.SettingsSource @SettingsSource}
         */
        public IConfig use(Class<?> clazz) throws SettingsException {
            return use(extractSource(clazz));
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
        public <T> T get(Class<T> clazz) throws SettingsException {
            return use(clazz).get(clazz);
        }

        public IMutableConfig track(Class<?> clazz) throws SettingsException, IOException, UnsupportedOperationException {
            return track(extractSource(clazz), clazz.getClassLoader());
        }

        public IMutableConfig track(String resourceName) throws IOException, UnsupportedOperationException {
            return track(resourceName, Config.class.getClassLoader());
        }

        public IMutableConfig track(String resourceName, ClassLoader classLoader) throws IOException, UnsupportedOperationException {
            try {
                final URL resource = classLoader.getResource(resourceName);
                if (resource == null) {
                    throw new FileNotFoundException("Resource " + resourceName + " is not found in class path");
                }
                if (!"file".equals(resource.getProtocol())) {
                    throw new IOException("Only resources as local files could be tracked.");
                }
                final Path path = Paths.get(resource.toURI());
                return track(path);
            } catch (URISyntaxException e) {
                throw new IOException("Failed to get URI for the resource " + resourceName, e);
            }
        }

        public IMutableConfig track(Path file) throws IOException, UnsupportedOperationException {
            if (file == null) {
                throw new NullPointerException("File can't be null");
            }
            if (EXCEPTION != null) {
                throw EXCEPTION;
            }

            return WATCHING_DAEMON.watch(file, prefixHandlers, substitutions);
        }

        public IMutableConfig track(File file) throws IOException, UnsupportedOperationException {
            if (file == null) {
                throw new NullPointerException("File can't be null");
            }
            return track(file.toPath());
        }
    }

}
