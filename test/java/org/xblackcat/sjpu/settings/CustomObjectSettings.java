package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.ParseWith;
import org.xblackcat.sjpu.settings.converter.InetAddressParser;

import java.net.InetAddress;

/**
 * 07.05.2015 10:25
 *
 * @author xBlackCat
 */
public interface CustomObjectSettings {
    @ParseWith(InetAddressParser.class)
    InetAddress getHost();

    @ParseWith(InetAddressParser.class)
    InetAddress[] getHostList();
}
