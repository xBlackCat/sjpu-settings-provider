package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.config.AConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
* 14.04.2014 17:07
*
* @author xBlackCat
*/
class FakeConfig extends AConfig {
    @Override
    protected Map<String, String> loadProperties() throws IOException, SettingsException {
        final Map<String, String> properties = new HashMap<>();
        properties.put("not.annotated", "true");
        properties.put("wrong.annotated", "true");
        return properties;
    }
}
