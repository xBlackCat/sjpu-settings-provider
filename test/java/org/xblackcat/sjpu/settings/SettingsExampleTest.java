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
        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(Settings2.class, "test2")
                    .and(Settings.class, "test1")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            final Settings settings = Config.use(vsf::getAsInputStream).get(Settings.class, "test1");
            final Settings2 settings2 = Config.use(vsf::getAsInputStream).get(Settings2.class, "test2");
        }

        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(SettingsWithDefault.class, "def")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsWithDefault.class, "def");
        }
    }

    @Test
    public void makeComplexExample() throws IOException, SettingsException {
        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(ComplexSettingsAuto.class, "prefix")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ComplexSettingsAuto.class, "prefix");
        }
    }

    @Test
    public void makeSubSettingsExample() throws IOException, SettingsException {
        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(IOptionalSubSettings.class, "subsettings")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(IOptionalSubSettings.class, "subsettings");
        }

        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(SettingsOptionalIgnore.class, "optional")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsOptionalIgnore.class, "optional");
        }
    }

    @Test
    public void makeCustomObjectExample() throws IOException, SettingsException {
        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(CustomObjectSettings.class, "custom")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(CustomObjectSettings.class, "custom");
        }
    }

    @Test
    public void makeGroupingExample() throws IOException, SettingsException {
        try (VirtualSettingsFile vsf = new VirtualSettingsFile()) {
            Example.of(ISubSettingsGroups.class, "groups")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .withDefault("groups.mandatory.int.val", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ISubSettingsGroups.class, "groups");
        }
    }
}
