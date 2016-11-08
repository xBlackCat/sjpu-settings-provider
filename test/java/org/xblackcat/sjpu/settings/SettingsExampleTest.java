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
        System.out.println("---------------------------------- Debug ------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(Settings2.class, "test2")
                    .and(Settings.class, "test1")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .withDefault("test1.simple.name", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            final Settings settings = Config.use(vsf::getAsInputStream).get(Settings.class, "test1");
            final Settings2 settings2 = Config.use(vsf::getAsInputStream).get(Settings2.class, "test2");
        }
        System.out.println("---------------------------------- Normal -----------------------------------------------");

        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(Settings2.class, "test2")
                    .and(Settings.class, "test1")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDefault("test1.simple.name", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            final Settings settings = Config.use(vsf::getAsInputStream).get(Settings.class, "test1");
            final Settings2 settings2 = Config.use(vsf::getAsInputStream).get(Settings2.class, "test2");
        }
        System.out.println("---------------------------------- Brief ------------------------------------------------");

        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(Settings2.class, "test2")
                    .and(Settings.class, "test1")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .brief()
                    .withDefault("test1.simple.name", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            final Settings settings = Config.use(vsf::getAsInputStream).get(Settings.class, "test1");
            final Settings2 settings2 = Config.use(vsf::getAsInputStream).get(Settings2.class, "test2");
        }
        System.out.println("---------------------------------- Pure -------------------------------------------------");

        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(Settings2.class, "test2")
                    .and(Settings.class, "test1")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .pure()
                    .withDefault("test1.simple.name", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            final Settings settings = Config.use(vsf::getAsInputStream).get(Settings.class, "test1");
            final Settings2 settings2 = Config.use(vsf::getAsInputStream).get(Settings2.class, "test2");
        }
        System.out.println("---------------------------------- Debug ------------------------------------------------");

        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(SettingsWithDefault.class, "def")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsWithDefault.class, "def");
        }
        System.out.println("---------------------------------- Brief ------------------------------------------------");

        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(SettingsWithDefault.class, "def")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .brief()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsWithDefault.class, "def");
        }
    }

    @Test
    public void makeComplexExample() throws IOException, SettingsException {
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ComplexSettingsAuto.class, "prefix")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .withDefault("prefix.number.set", "Two,Three")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ComplexSettingsAuto.class, "prefix");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ComplexSettingsAuto.class, "prefix")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ComplexSettingsAuto.class, "prefix");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ComplexSettingsAuto.class, "prefix")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ComplexSettingsAuto.class, "prefix");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ComplexSettingsAuto.class, "prefix")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .brief()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ComplexSettingsAuto.class, "prefix");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ComplexSettingsAuto.class, "prefix")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .pure()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ComplexSettingsAuto.class, "prefix");
        }
    }

    @Test
    public void makeSubSettingsExample() throws IOException, SettingsException {
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(IOptionalSubSettings.class, "subsettings")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(IOptionalSubSettings.class, "subsettings");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(IOptionalSubSettings.class, "subsettings")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(IOptionalSubSettings.class, "subsettings");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(IOptionalSubSettings.class, "subsettings")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .brief()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(IOptionalSubSettings.class, "subsettings");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(IOptionalSubSettings.class, "subsettings")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .pure()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(IOptionalSubSettings.class, "subsettings");
        }
        System.out.println("-----------------------------------------------------------------------------------------");

        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(SettingsOptionalIgnore.class, "optional")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsOptionalIgnore.class, "optional");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(SettingsOptionalIgnore.class, "optional")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsOptionalIgnore.class, "optional");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(SettingsOptionalIgnore.class, "optional")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .brief()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsOptionalIgnore.class, "optional");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(SettingsOptionalIgnore.class, "optional")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .pure()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(SettingsOptionalIgnore.class, "optional");
        }
    }

    @Test
    public void makeCustomObjectExample() throws IOException, SettingsException {
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(CustomObjectSettings.class, "custom")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDefault("custom.host.list", "127.0.0.1,localhost")
                    .withDebugInfo()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(CustomObjectSettings.class, "custom");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(CustomObjectSettings.class, "custom")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDefault("custom.host.list", "127.0.0.1,localhost")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(CustomObjectSettings.class, "custom");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(CustomObjectSettings.class, "custom")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDefault("custom.host.list", "127.0.0.1,localhost")
                    .brief()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(CustomObjectSettings.class, "custom");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(CustomObjectSettings.class, "custom")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDefault("custom.host.list", "127.0.0.1,localhost")
                    .pure()
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(CustomObjectSettings.class, "custom");
        }
    }

    @Test
    public void makeGroupingExample() throws IOException, SettingsException {
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ISubSettingsGroups.class, "groups")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDebugInfo()
                    .withDefault("groups.mandatory.int.val", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ISubSettingsGroups.class, "groups");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ISubSettingsGroups.class, "groups")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .withDefault("groups.mandatory.int.val", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ISubSettingsGroups.class, "groups");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ISubSettingsGroups.class, "groups")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .brief()
                    .withDefault("groups.mandatory.int.val", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ISubSettingsGroups.class, "groups");
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        try (VirtualFile vsf = new VirtualFile()) {
            Example.of(ISubSettingsGroups.class, "groups")
                    .withHeader("Header")
                    .withFooter("Footer")
                    .pure()
                    .withDefault("groups.mandatory.int.val", "1")
                    .writeTo(vsf);

            vsf.print(System.out);

            Config.use(vsf::getAsInputStream).get(ISubSettingsGroups.class, "groups");
        }
    }
}
