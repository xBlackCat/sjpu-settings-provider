package org.xblackcat.sjpu.settings.converter;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 07.05.2015 10:21
 *
 * @author xBlackCat
 */
public class InetAddressParser implements IParser<InetAddress> {
    @Override
    public Class<InetAddress> getReturnType() {
        return InetAddress.class;
    }

    @Override
    public InetAddress apply(String s) throws IllegalArgumentException {
        try {
            return InetAddress.getByName(s);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Can't parse host", e);
        }
    }

    @Override
    public String formatDescription() {
        return "Textual representation of IP or hostname";
    }
}
