package org.xblackcat.sjpu.settings;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * 10.01.13 11:37
 *
 * @author xBlackCat
 */
public class SettingsProviderTest {
    @Test
    public void generatePropertyNames() {
        final Method[] methods = Settings.class.getMethods();
        Assert.assertEquals("simple.name", ClassUtils.buildPropertyName(null, methods[0]));
        Assert.assertEquals("complex.name.with.abbr", ClassUtils.buildPropertyName(null, methods[1]));

    }

    @Test
    public void loadSettings() throws SettingsException, IOException, URISyntaxException {
        {
            Settings settings = SettingsProvider.get(Settings.class);

            Assert.assertEquals(1, settings.getSimpleName());
            Assert.assertEquals(42, settings.getComplexNameWithABBR());
        }
        {
            SettingsBlank testSettings = SettingsProvider.get(SettingsBlank.class, "/source/settings.properties");

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }
        {
            final URL resource = getClass().getResource("/source/settings.properties");
            SettingsBlank testSettings = SettingsProvider.get(
                    SettingsBlank.class,
                    resource.toURI()
            );

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }
        {
            SettingsPrefix testSettings = SettingsProvider.get(SettingsPrefix.class);

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }

        // Default value is not set for primitive field
        try {
            SettingsProvider.get(Settings.class, "/source/settings-blank.properties");
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
        try {
            SettingsProvider.get(SettingsBlank.class, "/source/settings-blank.properties");
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
    }

    @Test
    public void noSettings() throws SettingsException {
        {
            Assert.assertNull(SettingsProvider.get(SettingsBlank.class, "/no-settings.properties"));

            Assert.assertNull(SettingsProvider.get(SettingsBlank.class, "/no-settings.properties", true));
        }

    }

    @Test
    public void combinedSettings() throws SettingsException, IOException {
        final CombinedSettings settings;
        try (InputStream is = getClass().getResourceAsStream("/source/combined-settings.properties")) {
            settings = SettingsProvider.get(CombinedSettings.class, is);
        }

        Assert.assertEquals(1, settings.getSimpleName());
        Assert.assertEquals(42, settings.getComplexNameWithABBR());
        Assert.assertEquals("Test", settings.getValue());
        Assert.assertEquals("Another", settings.getAnotherValue());
    }

    @Test
    public void complexSettings() throws SettingsException, IOException {
        ComplexSettings settings = SettingsProvider.get(ComplexSettings.class);

        Assert.assertArrayEquals(new int[]{1, 10, 20, 50, 500, 1000}, settings.getIds());
        Assert.assertArrayEquals(new Numbers[]{Numbers.One, Numbers.Three, Numbers.Seven}, settings.getValues());

        Assert.assertEquals(
                Arrays.asList(Numbers.One, Numbers.Three, Numbers.Seven, Numbers.One, Numbers.Three, Numbers.Seven),
                settings.getNumberList()
        );
        Assert.assertEquals(
                new HashSet<>(Arrays.asList(Numbers.One, Numbers.Three, Numbers.Seven)),
                settings.getNumberSet()
        );
        Assert.assertEquals(
                EnumSet.of(Numbers.One, Numbers.Three, Numbers.Seven),
                settings.getNumberSet()
        );

        Map<Numbers, String> map1 = new EnumMap<>(Numbers.class);
        map1.put(Numbers.One, "Test-one");
        map1.put(Numbers.Two, "Test-two");
        map1.put(Numbers.Nine, "Test-9");

        Map<Long, Numbers> map2 = new HashMap<>();
        map2.put(2l, Numbers.Two);
        map2.put(3l, Numbers.Three);
        map2.put(4l, Numbers.Four);
        map2.put(5l, Numbers.Five);

        Assert.assertEquals(map1, settings.getNumberMap());
        Assert.assertEquals(map2, settings.getOtherNumberMap());
    }

    @Test
    public void invalidComplexSettings() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("not.annotated", "true");
        properties.put("wrong.annotated", "true");
        try {
            SettingsProvider.loadValues(InvalidComplexSettings1.class, properties, null);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
        try {
            SettingsProvider.loadValues(InvalidComplexSettings2.class, properties, null);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
        try {
            SettingsProvider.loadValues(InvalidComplexSettings3.class, properties, null);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
        try {
            SettingsProvider.loadValues(InvalidComplexSettings4.class, properties, null);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
    }
}
