package org.xblackcat.sjpu.settings.converter;

import java.net.URI;

/**
 * 07.05.2015 10:21
 *
 * @author xBlackCat
 */
public class URIParser implements IParser<URI> {
    @Override
    public Class<URI> getReturnType() {
        return URI.class;
    }

    @Override
    public URI apply(String s) throws IllegalArgumentException {
        return URI.create(s);
    }

    @Override
    public String formatDescription() {
        return "[scheme:]scheme-specific-part[#fragment]";
    }
}
