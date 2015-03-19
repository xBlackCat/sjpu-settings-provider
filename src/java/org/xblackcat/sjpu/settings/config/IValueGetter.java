package org.xblackcat.sjpu.settings.config;

import java.util.Collections;
import java.util.Set;

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

    String get(String key);

    Set<String> keySet();
}
