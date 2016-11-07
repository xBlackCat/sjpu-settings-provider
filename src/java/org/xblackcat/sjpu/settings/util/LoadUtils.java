package org.xblackcat.sjpu.settings.util;

import org.xblackcat.sjpu.settings.config.InputStreamConfig;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 03.11.2016 15:08
 *
 * @author xBlackCat
 */
public class LoadUtils {
    public static IValueGetter loadProperties(SupplierEx<InputStream, IOException> inputStreamProvider) throws IOException {
        if (inputStreamProvider == null) {
            return null;
        }

        final Map<String, String> shadow = new LinkedHashMap<>();
        // Use properties object for loading values only
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final Properties properties = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                return shadow.put(Objects.toString(key, null), Objects.toString(value, null));
            }
        };
        try (final InputStream in = inputStreamProvider.get()) {
            if (in == null) {
                return null;
            }
            properties.load(new InputStreamReader(new BufferedInputStream(in), StandardCharsets.UTF_8));
        }

        return wrap(shadow);
    }

    public static MapWrapper wrap(Map<String, String> shadow) {
        return new MapWrapper(shadow);
    }

    public static InputStream getInputStream(Path file) throws IOException {
        if (!Files.isReadable(file)) {
            throw new IOException("Read access denied for file " + file);
        }

        return Files.newInputStream(file, StandardOpenOption.READ);
    }

    public static InputStream buildInputStreamProvider(String resourceName) throws IOException {
        if (resourceName == null) {
            return null;
        }

        if (!resourceName.endsWith(".properties")) {
            InputStream is = getInputStream(resourceName + ".properties");
            if (is != null) {
                return is;
            }
        }

        return getInputStream(resourceName);
    }

    public static InputStream getInputStream(String propertiesFile) throws IOException {
        InputStream is = InputStreamConfig.class.getResourceAsStream(propertiesFile);
        if (is == null) {
            is = InputStreamConfig.class.getClassLoader().getResourceAsStream(propertiesFile);
        }

        return is;
    }

    public static InputStream getInputStream(File file) throws IOException {
        if (file == null) {
            return null;
        }

        if (!file.canRead()) {
            throw new IOException("Read access denied for file " + file);
        }

        return new FileInputStream(file);
    }
}
