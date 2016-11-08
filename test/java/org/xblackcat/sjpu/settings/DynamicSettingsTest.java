package org.xblackcat.sjpu.settings;

import org.junit.Ignore;
import org.junit.Test;
import org.xblackcat.sjpu.settings.config.IConfigListener;
import org.xblackcat.sjpu.settings.config.IMutableConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 03.11.2016 14:56
 *
 * @author xBlackCat
 */
@Ignore
public class DynamicSettingsTest {
    @Test
    public void workflow() throws IOException, InterruptedException {
        final Path file = Paths.get("R:/settings/1.properties");
        final Path file1 = Paths.get("R:/settings/1.properties");
        final Path file2 = Paths.get("R:/settings/2.properties");
        Config.track(file);
        Config.track(file1);
        Config.track(file2);
        Thread.sleep(200000);
        file.getParent();
    }

    @Test
    public void workflow2() throws IOException, InterruptedException, SettingsException {
        final Path file = Paths.get("R:/settings/1.properties");
        IMutableConfig config = Config.track(file);
        config.addListener(
                (clazz, prefix, newConfig) ->
                        System.out.println("Config updated! Class: " + clazz.getName() + "/" + prefix + ": " + newConfig)
        );
        Settings test = config.get(Settings.class, "test", true);
        Settings testCopy = config.get(Settings.class, "test", true);
        SettingsWithDefault test1 = config.get(SettingsWithDefault.class, "super");
        IOptionalSubSettings subSettings = config.get(IOptionalSubSettings.class);


        System.out.println("Initial data:\n --> " + test + "\n --> " + test1 + "\n --> " + subSettings);
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(
                        () -> {
                            System.out.println(
                                    "Data probe at " + LocalDateTime.now() + ":\n --> " + test +
                                            "\n --> " + testCopy +
                                            "\n --> " + test1 +
                                            "\n --> " + subSettings);
                            try {
                                System.out.println(" ==> " + test.getSimpleName());
                            } catch (NotLoadedException e) {
                                System.out.println("Got exception: " + e.getMessage());
                            }
                            try {
                                System.out.println(" ==> " + testCopy.getSimpleName());
                            } catch (NotLoadedException e) {
                                System.out.println("Got exception: " + e.getMessage());
                            }
                            try {
                                System.out.println(" ==> " + test1.getSimpleName());
                            } catch (NotLoadedException e) {
                                System.out.println("Got exception: " + e.getMessage());
                            }
                            try {
                                System.out.println(" ==> " + subSettings.getName());
                            } catch (NotLoadedException e) {
                                System.out.println("Got exception: " + e.getMessage());
                            }
                        },
                        1,
                        10,
                        TimeUnit.SECONDS
                );
        Thread.sleep(200000);
    }

    @Test
    public void workflow3() throws IOException, InterruptedException, SettingsException {
        final Path file = Paths.get("R:/settings/1.properties");
        final Path file1 = Paths.get("R:/settings/2.properties");
        IMutableConfig config = Config.track(file);
        IMutableConfig config2 = Config.track(file1);
        IConfigListener listener = (clazz, prefix, newConfig) ->
                System.out.println("Config updated! Class: " + clazz.getName() + "/" + prefix + ": " + newConfig);
        config.addListener(listener);
        config2.addListener(listener);

        SettingsWithDefault test = config.get(SettingsWithDefault.class, "test");
        SettingsWithDefault test1 = config2.get(SettingsWithDefault.class, "super");

        System.out.println("Initial data:\n --> " + test + "\n --> " + test1);
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(
                        () -> System.out.println("Data probe at " + LocalDateTime.now() + ":\n --> " + test + "\n --> " + test1),
                        1,
                        10,
                        TimeUnit.SECONDS
                );
        Thread.sleep(200000);
    }
}
