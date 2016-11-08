package org.xblackcat.sjpu.settings.config;

import java.util.Objects;

/**
 * 03.11.2016 15:38
 *
 * @author xBlackCat
 */
public class ConfigInfo<T> {
    private final Class<T> clazz;
    private final String prefix;
    private final boolean optional;

    public ConfigInfo(Class<T> clazz, String prefix) {
        this(clazz, prefix, false);
    }

    public ConfigInfo(Class<T> clazz, String prefix, boolean optional) {
        this.clazz = clazz;
        this.prefix = prefix;
        this.optional = optional;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ConfigInfo<?> that = (ConfigInfo<?>) o;
        return Objects.equals(clazz, that.clazz) &&
                Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, prefix);
    }

    @Override
    public String toString() {
        return clazz.getName() + " [prefix: " + prefix + "]";
    }
}
