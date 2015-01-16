package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.CollectionOf;
import org.xblackcat.sjpu.settings.ann.MapOf;
import org.xblackcat.sjpu.settings.ann.SettingsSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 15.10.13 16:28
 *
 * @author xBlackCat
 */
@SettingsSource("/source/complex.properties")
public interface ComplexSettings {
    int[] getIds();

    boolean[] getFlags();

    Numbers[] getValues();

    @CollectionOf(Numbers.class)
    List<Numbers> getNumberList();

    @CollectionOf(Numbers.class)
    Set<Numbers> getNumberSet();

    @MapOf(key = Numbers.class, value = String.class)
    Map<Numbers, String> getNumberMap();

    @MapOf(key = Long.class, value = Numbers.class)
    Map<Long, Numbers> getOtherNumberMap();
}
