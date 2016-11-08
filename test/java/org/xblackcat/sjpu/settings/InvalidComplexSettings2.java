package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.CollectionOf;

import java.util.ArrayList;

/**
 * 15.10.13 18:06
 *
 * @author xBlackCat
 */
public interface InvalidComplexSettings2 {
    @CollectionOf(String.class)
    ArrayList<String> wrongAnnotated();
}
