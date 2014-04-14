package org.xblackcat.sjpu.settings.config;

import java.io.*;
import java.util.Map;

/**
 * 14.04.2014 15:07
 *
 * @author xBlackCat
 */
public class FileConfig extends AnInputStreamConfig {
    private final File file;

    public FileConfig(File file) {
        this.file = file;
    }

    @Override
    protected Map<String, String> loadProperties() throws IOException {
        if (file == null) {
            return null;
        }

        if (!file.canRead()) {
            throw new IOException("Read access denied for file " + file);
        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return loadPropertiesFromStream(is);
        }
    }
}
