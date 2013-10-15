package org.xblackcat.sjpu.settings;

import java.util.List;
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

    @ListOf(Numbers.class)
    List<Numbers> getNumberList();

    @SetOf(Numbers.class)
    Set<Numbers> getNumberSet();
}
