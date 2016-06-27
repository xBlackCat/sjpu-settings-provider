package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.ParseWith;
import org.xblackcat.sjpu.settings.converter.URLParser;

import java.net.URL;

/**
 * Optional settings with not filled a value. Should be null in {@linkplain IOptionalSubSettings#getSubSettings()}
 *
 * @author xBlackCat
 */
public interface ISubSettings {
    int getIntVal();

    @Optional
    String getValue();

    @Optional
    @ParseWith(URLParser.class)
    URL getUrl();
}
