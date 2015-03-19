package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.*;

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

    @MapKey(Numbers.class)
    @MapValue(String.class)
    Map<Numbers, String> getNumberMap();

    @MapKey(Long.class)
    @MapValue(Numbers.class)
    @Splitter("=>")
    Map<Long, Numbers> getOtherNumberMap();
}
