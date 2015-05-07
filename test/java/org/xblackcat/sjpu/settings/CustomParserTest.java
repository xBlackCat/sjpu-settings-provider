package org.xblackcat.sjpu.settings;

import org.junit.Test;

/**
 * 07.05.2015 10:27
 *
 * @author xBlackCat
 */
public class CustomParserTest {
    @Test
    public void customObjectTest() throws SettingsException {
        CustomObjectSettings s = Config.use("/source/custom-object.properties").get(CustomObjectSettings.class);

        s.getHost();
    }

    @Test
    public void customObjectFailTest() throws SettingsException {
        // TODO: write normal tests
        CustomObjectSettingsFail s = Config.use("/source/custom-object.properties").get(CustomObjectSettingsFail.class);

        s.getHost();
    }
}
