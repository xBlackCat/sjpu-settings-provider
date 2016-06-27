package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.ann.ParseWith;
import org.xblackcat.sjpu.settings.ann.PropertyName;
import org.xblackcat.sjpu.settings.converter.InetAddressParser;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

/**
 * 07.05.2015 10:25
 *
 * @author xBlackCat
 */
public interface CustomObjectSettings {
    @ParseWith(InetAddressParser.class)
    @Optional
    InetAddress getHost();

    @ParseWith(InetAddressParser.class)
    @PropertyName("host.list")
    InetAddress[] getHostArray();

    @ParseWith(InetAddressParser.class)
    @PropertyName("host.list2")
    List<InetAddress> getHostList();

    @ParseWith(InetAddressParser.class)
    @PropertyName("host.set")
    Set<InetAddress> getHostSet();
}
