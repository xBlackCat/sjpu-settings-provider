package org.xblackcat.sjpu.settings;

import org.junit.Assert;
import org.junit.Test;
import org.xblackcat.sjpu.settings.config.IConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * 25.06.2018 14:40
 *
 * @author xBlackCat
 */
public class SubstitutionTest {
    @Test
    public void simpleSubstitution() throws SettingsException {
        String userHome = System.getProperty("user.home");
        String userName = System.getProperty("user.name");
        {
            Settings2 s = Config.use("source/substitution-settings.properties").get(Settings2.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${sub.value}/${user.name}", s.getAnotherValue());
        }
        {
            Settings2 s = Config.substitute(Collections.emptyMap())
                    .use("source/substitution-settings.properties")
                    .get(Settings2.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${sub.value}/${user.name}", s.getAnotherValue());
        }
        {
            Settings2 s = Config.substitute(Collections.singletonMap("user.name", "Hello"))
                    .use("source/substitution-settings.properties")
                    .get(Settings2.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${sub.value}/${user.name}", s.getAnotherValue());
        }
        {
            Settings2 s = Config.substitute(Collections.singletonMap("user.name", "${user.home}"))
                    .use("source/substitution-settings.properties")
                    .get(Settings2.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${sub.value}/${user.name}", s.getAnotherValue());
        }
        {
            SettingsSubstitution s = Config.use("source/substitution-settings.properties")
                    .get(SettingsSubstitution.class, "sub");

            Assert.assertEquals(userHome, s.getValue());
            Assert.assertEquals(userHome + "/" + userName, s.getAnotherValue());
        }
        {
            SettingsSubstitution s = Config.substitute(Collections.emptyMap())
                    .use("source/substitution-settings.properties")
                    .get(SettingsSubstitution.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${user.home}/${user.name}", s.getAnotherValue());
        }
        {
            SettingsSubstitution s = Config.substitute(Collections.singletonMap("user.name", "Hello"))
                    .use("source/substitution-settings.properties")
                    .get(SettingsSubstitution.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${user.home}/Hello", s.getAnotherValue());
        }
        {
            SettingsSubstitution s = Config.substitute(Collections.singletonMap("user.name", "${user.home}"))
                    .use("source/substitution-settings.properties")
                    .get(SettingsSubstitution.class, "sub");

            Assert.assertEquals("${user.home}", s.getValue());
            Assert.assertEquals("${user.home}/${user.home}", s.getAnotherValue());
        }
    }

    @Test
    public void configSubstitution() throws SettingsException {
        {
            IConfig c1 = Config.use("source/substitution-i-settings-1.properties");
            IConfig c2 = Config.substitute(c1).use("source/substitution-i-settings-2.properties");

            SettingsSubstitution s1 = c1.get(SettingsSubstitution.class, "sub1");
            SettingsSubstitution s2 = c2.get(SettingsSubstitution.class, "sub2");

            Assert.assertEquals("Sub-1", s1.getValue());
            Assert.assertEquals("Sub-1/Test", s1.getAnotherValue());
            Assert.assertEquals("Sub-1/Test--22", s2.getValue());
            Assert.assertEquals("Sub-1-33", s2.getAnotherValue());
        }
        {
            IConfig c1 = Config.use("source/substitution-i-settings-1.properties");
            IConfig c2 = Config.substitute(c1).use("source/substitution-i-settings-2.properties");
            IConfig c3 = Config.substitute(c1).substitute(c2).use("source/substitution-i-settings-3.properties");

            SettingsSubstitution s1 = c1.get(SettingsSubstitution.class, "sub1");
            SettingsSubstitution s2 = c2.get(SettingsSubstitution.class, "sub2");
            SettingsSubstitution s3 = c3.get(SettingsSubstitution.class, "sub3");

            Assert.assertEquals("Sub-1", s1.getValue());
            Assert.assertEquals("Sub-1/Test", s1.getAnotherValue());
            Assert.assertEquals("Sub-1/Test--22", s2.getValue());
            Assert.assertEquals("Sub-1-33", s2.getAnotherValue());
            Assert.assertEquals("Sub-1-33-3", s3.getValue());
            Assert.assertEquals("Sub-1-33-3-Sub-1/Test--22-Sub-1", s3.getAnotherValue());
        }
    }

    @Test
    public void recursionTest() throws SettingsException {
        try {
            IConfig c1 = Config.use("source/substitution-recurrent-simple.properties");
            SettingsSubstitution s1 = c1.get(SettingsSubstitution.class, "sub");
            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Recurrent reference to a property sub.value", e.getMessage());
        }

        try {
            IConfig c1 = Config.use("source/substitution-recurrent-1.properties");
            IConfig c2 = Config.substitute(c1).use("source/substitution-recurrent-2.properties");
            SettingsSubstitution s1 = c1.get(SettingsSubstitution.class, "sub1");
            SettingsSubstitution s2 = c2.get(SettingsSubstitution.class, "sub2");

            Assert.fail("Exception is expected");
        } catch (SettingsException e) {
            Assert.assertEquals("Recurrent reference to a property sub2.value", e.getMessage());
        }
    }

    @Test
    public void prefixProcessingTest() throws SettingsException {
        {
            Settings s = Config.with("BASE64:", v -> new String(Base64.getDecoder().decode(v), StandardCharsets.UTF_8))
                    .use("source/prefix-settings.properties").get(Settings.class);

            Assert.assertEquals(1, s.getSimpleName());
            Assert.assertEquals(42, s.getComplexNameWithABBR());
        }
        {
            Settings2 s = Config.use("source/prefix-settings-2.properties").get(Settings2.class);

            Assert.assertEquals("BASE64:VEVTVA==", s.getValue());
            Assert.assertEquals("BASE64:QW5vdGhlciB0ZXN0IHN0cmluZw==", s.getAnotherValue());
        }
        {
            Settings2 s = Config.with("BASE64:", v -> new String(Base64.getDecoder().decode(v), StandardCharsets.UTF_8))
                    .use("source/prefix-settings-2.properties").get(Settings2.class);

            Assert.assertEquals("TEST", s.getValue());
            Assert.assertEquals("Another test string", s.getAnotherValue());
        }
    }
}
