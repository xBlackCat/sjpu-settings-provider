package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.ListOf;

import java.util.Set;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings3 {
    @ListOf(String.class)
    Set<String> wrongAnnotated();
}
