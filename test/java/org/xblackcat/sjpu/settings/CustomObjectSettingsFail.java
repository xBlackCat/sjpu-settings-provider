package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.CollectionOf;
import org.xblackcat.sjpu.settings.ann.Ignore;
import org.xblackcat.sjpu.settings.ann.ParseWith;
import org.xblackcat.sjpu.settings.ann.PropertyName;
import org.xblackcat.sjpu.settings.converter.InetAddressParser;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;

/**
 * 07.05.2015 10:25
 *
 * @author xBlackCat
 */
public interface CustomObjectSettingsFail {
    @ParseWith(InetAddressParser.class)
    InetAddress getHost();

    @Ignore
    @ParseWith(InetAddressParser.class)
    @PropertyName("host.list")
    Inet4Address[] getHostArray();

    @Ignore
    @ParseWith(InetAddressParser.class)
    @PropertyName("host.list")
    @CollectionOf(Number.class)
    List<InetAddress> getHostList();

    @Ignore
    @ParseWith(InetAddressParser.class)
    @PropertyName("host.list")
    Set<Inet4Address> getHostSet();
}
