package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.CollectionOf;

import java.util.Set;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings3 {
    @CollectionOf(Double.class)
    Set<CharSequence> wrongAnnotated();
}
