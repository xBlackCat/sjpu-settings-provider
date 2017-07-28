import org.xblackcat.sjpu.settings.Config;
import org.xblackcat.sjpu.settings.SettingsException;

import java.net.MalformedURLException;

class SimpleExample {
    public static void main(String[] args) throws MalformedURLException {
        UserSettings settings;
        try {
            // Load from resources
            settings = Config.use("/user.properties").get(UserSettings.class);
            // Load from file
//            settings = Config.use(Paths.get("<path>/user.properties")).get(UserSettings.class);
            // Load by URL
//            settings = Config.use(new URL("file://<path>/user.properties")).get(UserSettings.class);
        } catch (SettingsException e) {
            // Failed to load or parse settings
            e.printStackTrace();
            return;
        }

        System.out.println("Loaded settings: " + settings);
    }
}