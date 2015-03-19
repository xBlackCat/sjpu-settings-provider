package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.DefaultValue;
import org.xblackcat.sjpu.settings.ann.Optional;

import java.util.List;
import java.util.Map;

/**
 * 24.07.13 13:29
 *
 * @author xBlackCat
 */
@Optional
public interface SettingsOptional {
    @Optional
    String getSimpleName();

    @Optional
    Numbers getNoNumber();

    @DefaultValue("one")
    Numbers getOneDefault();

    @Optional
    List<Numbers> getNoNumbers();

    @DefaultValue("")
    List<Numbers> getEmptyNumbers();

    @Optional
    Map<String, Numbers> getNoNumbersMap();

    @DefaultValue("")
    Map<String, Numbers> getEmptyNumbersMap();
}
