package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.CollectionOf;

import java.util.List;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings4 {
    @CollectionOf(String.class)
    List<Number> wrongAnnotated();
}
