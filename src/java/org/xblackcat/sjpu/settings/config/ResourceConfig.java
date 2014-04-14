package org.xblackcat.sjpu.settings.config;

import org.xblackcat.sjpu.settings.SettingsException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 14.04.2014 15:07
 *
 * @author xBlackCat
 */
public class ResourceConfig extends AnInputStreamConfig {
    private final String resourceName;

    public ResourceConfig(String resourceName) {
        this.resourceName = resourceName;
    }

    static InputStream getInputStream(String propertiesFile) throws IOException {
        InputStream is = ResourceConfig.class.getResourceAsStream(propertiesFile);
        if (is == null) {
            is = ResourceConfig.class.getClassLoader().getResourceAsStream(propertiesFile);
        }

        return is;
    }

    @Override
    protected Map<String, String> loadProperties() throws IOException, SettingsException {
        if (resourceName == null) {
            return null;
        }

        InputStream is = null;
        try {
            if (!resourceName.endsWith(".properties")) {
                is = getInputStream(resourceName + ".properties");
            }

            if (is == null) {
                is = getInputStream(resourceName);
            }

            if (is == null) {
                return null;
            }

            return loadPropertiesFromStream(is);
        } catch (IOException e) {
            throw new SettingsException("Can't load values from  resource " + resourceName, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("Can't close stream. [" + resourceName + "]", e);
                }
            }
        }
    }
}
