package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.DefaultValue;
import org.xblackcat.sjpu.settings.ann.Description;
import org.xblackcat.sjpu.settings.ann.SettingsSource;
import org.xblackcat.sjpu.settings.ann.Splitter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 15.10.13 16:28
 *
 * @author xBlackCat
 */
@SettingsSource("/source/complex.properties")
public interface ComplexSettingsAuto {
    int[] getIds();

    boolean[] getFlags();

    Numbers[] getValues();

    List<Numbers> getNumberList();

    @DefaultValue("One,Two,Three")
    @Description("Set of numbers")
    Set<Numbers> getNumberSet();

    @Description("Additional numbers map")
    Map<Numbers, String> getNumberMap();

    @Splitter("=>")
    Map<Long, Numbers> getOtherNumberMap();
}
