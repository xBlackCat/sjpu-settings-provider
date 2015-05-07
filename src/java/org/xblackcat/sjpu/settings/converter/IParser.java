package org.xblackcat.sjpu.settings.converter;

import java.util.function.Function;

/**
 * 07.05.2015 9:54
 *
 * @author xBlackCat
 */
public interface IParser<T> extends Function<String, T> {
    Class<T> getReturnType();
}
