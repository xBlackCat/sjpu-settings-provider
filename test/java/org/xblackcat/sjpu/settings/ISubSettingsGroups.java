package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.GroupField;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.PropertyName;

import java.util.Map;

/**
 * 08.12.2015 12:00
 *
 * @author xBlackCat
 */
public interface ISubSettingsGroups {
    @PropertyName("mandatory")
    @GroupField(ISubSettings.class)
    Map<String, ISubSettings> getMandatorySubSettings();

    @Optional
    @PropertyName("optional")
    @GroupField(ISubSettings.class)
    Map<String, ISubSettings> getOptionalSubSettings();

    String getName();
}
