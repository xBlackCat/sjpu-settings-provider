package org.xblackcat.sjpu.settings;

/**
 * 14.10.13 16:55
 *
 * @author xBlackCat
 */
public interface TestSettings2 {
    String getValue();

    @PropertyName("value2")
    String getAnotherValue();
}
