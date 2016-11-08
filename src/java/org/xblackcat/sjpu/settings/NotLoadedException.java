package org.xblackcat.sjpu.settings;

/**
 * 08.11.2016 10:31
 *
 * @author xBlackCat
 */
public class NotLoadedException extends IllegalStateException {
    public NotLoadedException() {
    }

    public NotLoadedException(String s) {
        super(s);
    }

    public NotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotLoadedException(Throwable cause) {
        super(cause);
    }
}
