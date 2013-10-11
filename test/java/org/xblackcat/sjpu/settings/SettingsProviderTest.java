package org.xblackcat.sjpu.settings;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 10.01.13 11:37
 *
 * @author xBlackCat
 */
public class SettingsProviderTest {
    @Test
    public void generatePropertyNames() {
        final Method[] methods = TestSettings.class.getMethods();
        Assert.assertEquals("simple.name", ClassUtils.buildPropertyName(null, methods[0]));
        Assert.assertEquals("complex.name.with.abbr", ClassUtils.buildPropertyName(null, methods[1]));

    }

    @Test
    public void loadSettings() throws SettingsException, IOException {
        {
            TestSettings testSettings = SettingsProvider.get(TestSettings.class);

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }
        {
            TestSettingsBlank testSettings = SettingsProvider.get(
                    TestSettingsBlank.class,
                    ClassUtils.getInputStream("/source/settings.properties")
            );

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }

        // Default value is not set for primitive field
        try {
            SettingsProvider.get(TestSettings.class, ClassUtils.getInputStream("/source/settings-blank.properties"));
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
        try {
            SettingsProvider.get(TestSettingsBlank.class, ClassUtils.getInputStream("/source/settings-blank.properties"));
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue( true);
        }
    }
}
