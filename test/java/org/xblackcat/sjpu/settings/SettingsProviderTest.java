package org.xblackcat.sjpu.settings;

import javassist.ClassPool;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.config.AConfig;
import org.xblackcat.sjpu.settings.config.ClassUtils;

import java.io.IOException;
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
    //    @Test
    public void showJvm() throws SettingsException {
        final Jvm jvm = Config.anyOf(Config.useEnv(), Config.useJvm()).get(Jvm.class);
        System.out.println("JavaVersion: " + jvm.getJavaVersion());
        System.out.println("JavaVendor: " + jvm.getJavaVendor());
        System.out.println("JavaVendorUrl: " + jvm.getJavaVendorUrl());
        System.out.println("JavaHome: " + jvm.getJavaHome());
        System.out.println("JavaVmSpecificationVersion: " + jvm.getJavaVmSpecificationVersion());
        System.out.println("JavaVmSpecificationVendor: " + jvm.getJavaVmSpecificationVendor());
        System.out.println("JavaVmSpecificationName: " + jvm.getJavaVmSpecificationName());
        System.out.println("JavaVmVersion: " + jvm.getJavaVmVersion());
        System.out.println("JavaVmVendor: " + jvm.getJavaVmVendor());
        System.out.println("JavaVmName: " + jvm.getJavaVmName());
        System.out.println("JavaSpecificationVersion: " + jvm.getJavaSpecificationVersion());
        System.out.println("JavaSpecificationVendor: " + jvm.getJavaSpecificationVendor());
        System.out.println("JavaSpecificationName: " + jvm.getJavaSpecificationName());
        System.out.println("JavaClassVersion: " + jvm.getJavaClassVersion());
        System.out.println("JavaClassPath: " + jvm.getJavaClassPath());
        System.out.println("JavaLibraryPath: " + jvm.getJavaLibraryPath());
        System.out.println("JavaIoTmpdir: " + jvm.getJavaIoTmpdir());
        System.out.println("JavaCompiler: " + jvm.getJavaCompiler());
        System.out.println("JavaExtDirs: " + jvm.getJavaExtDirs());
        System.out.println("OsName: " + jvm.getOsName());
        System.out.println("OsArch: " + jvm.getOsArch());
        System.out.println("OsVersion: " + jvm.getOsVersion());
        System.out.println("FileSeparator: " + jvm.getFileSeparator());
        System.out.println("PathSeparator: " + jvm.getPathSeparator());
        System.out.println("LineSeparator: " + StringEscapeUtils.escapeJava(jvm.getLineSeparator()));
        System.out.println("UserName: " + jvm.getUserName());
        System.out.println("UserHome: " + jvm.getUserHome());
        System.out.println("UserDir: " + jvm.getUserDir());
    }

    @Test
    public void generatePropertyNames() {
        final Method[] methods = Settings.class.getMethods();
        Assert.assertEquals("simple.name", ClassUtils.buildPropertyName(null, methods[0]));
        Assert.assertEquals("complex.name.with.abbr", ClassUtils.buildPropertyName(null, methods[1]));

    }

    @Test
    public void loadSettings() throws SettingsException, IOException, URISyntaxException {
        {
            Settings settings = Config.get(Settings.class);

            Assert.assertEquals(1, settings.getSimpleName());
            Assert.assertEquals(42, settings.getComplexNameWithABBR());
        }
        {
            SettingsBlank testSettings = Config.use("/source/settings.properties").get(SettingsBlank.class);

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }
        {
            final URL resource = getClass().getResource("/source/settings.properties");
            SettingsBlank testSettings = Config.use(resource).get(SettingsBlank.class);

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }
        {
            SettingsPrefix testSettings = Config.get(SettingsPrefix.class);

            Assert.assertEquals(1, testSettings.getSimpleName());
            Assert.assertEquals(42, testSettings.getComplexNameWithABBR());
        }

        {
            final SettingsOptional testSettings = Config.use("/source/settings-blank.properties").get(SettingsOptional.class);

            Assert.assertNull(testSettings.getSimpleName());
            Assert.assertNull(testSettings.getNoNumber());
            Assert.assertEquals(Numbers.One, testSettings.getOneDefault());
            Assert.assertNull(testSettings.getNoNumbers());
            Assert.assertNotNull(testSettings.getEmptyNumbers());
            Assert.assertEquals(0, testSettings.getEmptyNumbers().size());
            Assert.assertNull(testSettings.getNoNumbersMap());
            Assert.assertNotNull(testSettings.getEmptyNumbersMap());
            Assert.assertEquals(0, testSettings.getEmptyNumbersMap().size());
        }

        // Default value is not set for primitive field
        try {
            Config.use("/source/settings-blank.properties").get(Settings.class);
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            Assert.assertEquals(
                    "Default value should be set for primitive type with @SettingField annotation for method getSimpleName",
                    e.getMessage()
            );
        }
        try {
            Config.use("/source/settings-blank.properties").get(SettingsBlank.class);
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            Assert.assertEquals(
                    "Default value should be set for primitive type with @SettingField annotation for method getSimpleName",
                    e.getMessage()
            );
        }

        {
            final SettingsOptionalIgnore testSettings = Config.use("/source/settings-blank.properties").get(SettingsOptionalIgnore.class);

            Assert.assertNull(testSettings.getSimpleName());
            Assert.assertNull(testSettings.getNoNumber());
            Assert.assertEquals(Numbers.One, testSettings.getOneDefault());
            Assert.assertNull(testSettings.getNoNumbers());
            Assert.assertNotNull(testSettings.getEmptyNumbers());
            Assert.assertEquals(0, testSettings.getEmptyNumbers().size());
            Assert.assertNull(testSettings.getNoNumbersMap());
            Assert.assertNotNull(testSettings.getEmptyNumbersMap());
            Assert.assertEquals(0, testSettings.getEmptyNumbersMap().size());

            try {
                final int value = testSettings.getValue();
                Assert.fail("Expected NotImplemented exception");
            } catch (NotImplementedException e) {
                Assert.assertEquals("Method getValue is excluded from generation", e.getMessage());
            }

            try {
                testSettings.notImplemented();
                Assert.fail("Expected NotImplemented exception");
            } catch (NotImplementedException e) {
                Assert.assertEquals("Method notImplemented is excluded from generation", e.getMessage());
            }
        }

    }

    @Test
    public void noSettings() throws SettingsException {
        {
            Assert.assertNull(Config.use("/no-settings.properties").get(SettingsBlank.class));

            Assert.assertNull(Config.use("/no-settings.properties").get(SettingsBlank.class, true));
        }

    }

    @Test
    public void combinedSettings() throws SettingsException, IOException {
        final CombinedSettings settings = Config.use("/source/combined-settings.properties").get(CombinedSettings.class);

        Assert.assertEquals(1, settings.getSimpleName());
        Assert.assertEquals(42, settings.getComplexNameWithABBR());
        Assert.assertEquals("Test", settings.getValue());
        Assert.assertEquals("Another", settings.getAnotherValue());

        AConfig conf = Config.use("/source/combined-settings.properties");
        Settings s = conf.get(Settings.class);
        Settings2 s2 = conf.get(Settings2.class);

        Assert.assertEquals(1, s.getSimpleName());
        Assert.assertEquals(42, s.getComplexNameWithABBR());

        Assert.assertEquals("Test", s2.getValue());
        Assert.assertEquals("Another", s2.getAnotherValue());
    }

    @Test
    public void complexSettings() throws SettingsException, IOException {
        ComplexSettings settings = Config.get(ComplexSettings.class);

        Assert.assertArrayEquals(new int[]{1, 10, 20, 50, 500, 1000}, settings.getIds());
        Assert.assertTrue(Arrays.equals(new boolean[]{true, true, false, true, false, true, false, false}, settings.getFlags()));
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
    public void complexSettingsAuto() throws SettingsException, IOException {
        ComplexSettingsAuto settings = Config.get(ComplexSettingsAuto.class);

        Assert.assertArrayEquals(new int[]{1, 10, 20, 50, 500, 1000}, settings.getIds());
        Assert.assertTrue(Arrays.equals(new boolean[]{true, true, false, true, false, true, false, false}, settings.getFlags()));
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
        AConfig conf = new FakeConfig(new ClassPool(true));
        try {
            conf.get(InvalidComplexSettings1.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Only non-abstract classes could be specified as collection elements", e.getMessage());
        }
        try {
            conf.get(InvalidComplexSettings2.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals(
                    "Please, specify container by interface java.util.Collection, java.util.List or java.util.Set as return type for collections.",
                    e.getMessage()
            );
        }
        try {
            conf.get(InvalidComplexSettings3.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Specified return object java.lang.Double cannot be casted to java.lang.CharSequence", e.getMessage());
        }
        try {
            conf.get(InvalidComplexSettings4.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Specified return object java.lang.String cannot be casted to java.lang.Number", e.getMessage());
        }
        try {
            conf.get(InvalidComplexSettings5.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Unknown type to parse: java.lang.Number", e.getMessage());
        }
        try {
            conf.get(InvalidComplexSettings6.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Specified return object java.lang.Double cannot be casted to java.lang.CharSequence", e.getMessage());
        }
        try {
            conf.get(InvalidComplexSettings7.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Specified return object java.lang.String cannot be casted to java.lang.Number", e.getMessage());
        }
        try {
            conf.get(InvalidComplexSettings8.class);
            Assert.fail("Exception expected");
        } catch (SettingsException e) {
            Assert.assertEquals(
                    "Please, specify general interface for maps as return type for method public abstract java.util.HashMap org.xblackcat.sjpu.settings.InvalidComplexSettings8.wrongAnnotated()",
                    e.getMessage()
            );
        }
    }

    public interface Jvm {
        @Optional
        String getJavaVersion();

        @Optional
        String getJavaVendor();

        @Optional
        String getJavaVendorUrl();

        @Optional
        String getJavaHome();

        @Optional
        String getJavaVmSpecificationVersion();

        @Optional
        String getJavaVmSpecificationVendor();

        @Optional
        String getJavaVmSpecificationName();

        @Optional
        String getJavaVmVersion();

        @Optional
        String getJavaVmVendor();

        @Optional
        String getJavaVmName();

        @Optional
        String getJavaSpecificationVersion();

        @Optional
        String getJavaSpecificationVendor();

        @Optional
        String getJavaSpecificationName();

        @Optional
        String getJavaClassVersion();

        @Optional
        String getJavaClassPath();

        @Optional
        String getJavaLibraryPath();

        @Optional
        String getJavaIoTmpdir();

        @Optional
        String getJavaCompiler();

        @Optional
        String getJavaExtDirs();

        @Optional
        String getOsName();

        @Optional
        String getOsArch();

        @Optional
        String getOsVersion();

        @Optional
        String getFileSeparator();

        @Optional
        String getPathSeparator();

        @Optional
        String getLineSeparator();

        @Optional
        String getUserName();

        @Optional
        String getUserHome();

        @Optional
        String getUserDir();
    }
}
