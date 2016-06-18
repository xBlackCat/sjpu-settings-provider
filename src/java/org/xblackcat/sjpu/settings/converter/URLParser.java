package org.xblackcat.sjpu.settings.converter;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 07.05.2015 10:21
 *
 * @author xBlackCat
 */
public class URLParser implements IParser<URL> {
    @Override
    public Class<URL> getReturnType() {
        return URL.class;
    }

    @Override
    public URL apply(String s) throws IllegalArgumentException {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Filed to parse URL", e);
        }
    }

    @Override
    public String formatDescription() {
        return "URL in RFC 2396 format";
    }
}
