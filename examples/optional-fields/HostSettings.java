import org.xblackcat.sjpu.settings.ann.DefaultValue;
import org.xblackcat.sjpu.settings.ann.Optional;

/**
 * 28.07.2017 11:52
 *
 * @author xBlackCat
 */
public interface HostSettings {
    @Optional
    String getTargetHost();

    @DefaultValue("80")
    int getTargetPort();
}
