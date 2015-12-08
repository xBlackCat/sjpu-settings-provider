package org.xblackcat.sjpu.settings;

import org.junit.Assert;
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

    @Test
    public void optionalSubsettingsTest() throws SettingsException {
        {
            IOptionalSubSettings oss = Config.use("/source/subsettings-correct.properties").get(IOptionalSubSettings.class);

            Assert.assertEquals("correct", oss.getName());
            Assert.assertNotNull(oss.getSubSettings());
            Assert.assertEquals(1, oss.getSubSettings().getIntVal());
            Assert.assertEquals("value", oss.getSubSettings().getValue());
        }
        {
            IOptionalSubSettings oss = Config.use("/source/subsettings-nosubsettings.properties").get(IOptionalSubSettings.class);

            Assert.assertEquals("no subsettings", oss.getName());
            Assert.assertNull(oss.getSubSettings());
        }
        {
            IOptionalSubSettings oss = Config.use("/source/subsettings-partial.properties").get(IOptionalSubSettings.class);

            Assert.assertEquals("partial", oss.getName());
            Assert.assertNotNull(oss.getSubSettings());
            Assert.assertEquals(1, oss.getSubSettings().getIntVal());
            Assert.assertNull(oss.getSubSettings().getValue());
        }
        {
            IOptionalSubSettings oss = Config.use("/source/subsettings-partial-2.properties").get(IOptionalSubSettings.class);

            Assert.assertEquals("partial2", oss.getName());
            Assert.assertNull(oss.getSubSettings());
        }
    }
}
