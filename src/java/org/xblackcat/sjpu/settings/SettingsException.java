package org.xblackcat.sjpu.settings;

/**
 * 12.02.13 11:19
 *
 * @author xBlackCat
 */
public class SettingsException extends Exception {
    public SettingsException() {
    }

    public SettingsException(String message) {
        super(message);
    }

    public SettingsException(String message, Throwable cause) {
        super(message, cause);
    }

    public SettingsException(Throwable cause) {
        super(cause);
    }
}
