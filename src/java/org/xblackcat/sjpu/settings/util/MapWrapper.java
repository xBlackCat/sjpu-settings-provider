package org.xblackcat.sjpu.settings.util;

import java.util.Map;
import java.util.Set;

/**
 * 14.12.2014 21:26
 *
 * @author xBlackCat
 */
public class MapWrapper implements IValueGetter {
    private final Map<String, String> map;

    public MapWrapper(Map<String, String> map) {
        this.map = map;
    }

    public String get(String key) {
        return map.get(key);
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }
}
