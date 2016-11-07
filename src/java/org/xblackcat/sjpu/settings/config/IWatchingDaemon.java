package org.xblackcat.sjpu.settings.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 03.11.2016 16:13
 *
 * @author xBlackCat
 */
public interface IWatchingDaemon {
    void watch(Path file, MutableConfig mutableConfig) throws IOException;

    void postNotify(Runnable event);
}
