import org.xblackcat.sjpu.settings.Config;
import org.xblackcat.sjpu.settings.SettingsException;

import java.net.MalformedURLException;

class OptionalExample {
    public static void main(String[] args) throws MalformedURLException {
        HostSettings allSet;
        HostSettings defaultPort;
        HostSettings optionalHost;
        try {
            allSet = Config.use("/all-set.properties").get(HostSettings.class);
            defaultPort = Config.use("/default-port.properties").get(HostSettings.class);
            optionalHost = Config.use("/optional-host.properties").get(HostSettings.class);
        } catch (SettingsException e) {
            // Failed to load or parse settings
            e.printStackTrace();
            return;
        }

        System.out.println("All set: " + allSet);
        System.out.println("Default port: " + defaultPort);
        System.out.println("Optional host: " + optionalHost);

        System.out.println(allSet.getTargetHost() + ":" + allSet.getTargetPort());
    }
}