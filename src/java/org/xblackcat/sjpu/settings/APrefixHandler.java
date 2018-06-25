package org.xblackcat.sjpu.settings;

/**
 * 25.06.2018 10:50
 *
 * @author xBlackCat
 */
public abstract class APrefixHandler {
    private final String prefix;

    protected APrefixHandler(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    protected abstract String process(String value);
}
