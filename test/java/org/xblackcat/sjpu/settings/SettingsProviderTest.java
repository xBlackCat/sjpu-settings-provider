package org.xblackcat.sjpu.settings;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("simple.name", SettingsProvider.getPropertyName(null, methods[0]));
        Assert.assertEquals("complex.name.with.abbr", SettingsProvider.getPropertyName(null, methods[1]));

    }
}
