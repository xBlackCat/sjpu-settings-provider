package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.SetOf;

import java.util.List;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings4 {
    @SetOf(String.class)
    List<String> wrongAnnotated();
}
