package org.xblackcat.sjpu.settings.util;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 12.12.2014 19:03
 *
 * @author xBlackCat
 */
public interface IValueGetter {
    IValueGetter EMPTY = new IValueGetter() {
        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public Set<String> keySet() {
            return Collections.emptySet();
        }
    };

    static IValueGetter withPrefix(IValueGetter valueGetter, String prefix) {
        if (valueGetter == null) {
            return null;
        }
        return valueGetter.withPrefix(prefix);
    }

    String get(String key);

    Set<String> keySet();

    default IValueGetter withPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0) {
            return this;
        }
        return new IValueGetter() {
            @Override
            public String get(String key) {
                return IValueGetter.this.get(prefix + "." + key);
            }

            @Override
            public Set<String> keySet() {
                return IValueGetter.this.keySet().stream().map(v -> prefix + "." + v).collect(Collectors.toSet());
            }
        };
    }
}
