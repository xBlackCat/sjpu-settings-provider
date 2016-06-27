package org.xblackcat.sjpu.settings;

import org.junit.Test;

import java.io.IOException;

/**
 * 24.06.2016 16:30
 *
 * @author xBlackCat
 */
public class SettingsExampleTest {
    @Test
    public void makeSimpleExample() throws IOException, SettingsException {
        Example.of(Settings2.class, "test2")
                .and(Settings.class, "test1")
                .withHeader("Header")
                .withFooter("Footer")
                .withDebugInfo()
                .writeTo(System.out);

        Example.of(SettingsWithDefault.class, "def")
                .withHeader("Header")
                .withFooter("Footer")
                .withDebugInfo()
                .writeTo(System.out);
    }

    @Test
    public void makeComplexExample() throws IOException, SettingsException {
        Example.of(ComplexSettingsAuto.class, "prefix")
                .withHeader("Header")
                .withFooter("Footer")
                .withDebugInfo()
                .writeTo(System.out);
    }

    @Test
    public void makeSubSettingsExample() throws IOException, SettingsException {
        Example.of(IOptionalSubSettings.class, "optional")
                .withHeader("Header")
                .withFooter("Footer")
                .withDebugInfo()
                .writeTo(System.out);
    }
}
