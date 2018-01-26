package org.xblackcat.sjpu.settings.converter;

import java.time.LocalTime;

/**
 * 31.08.2016 10:00
 *
 * @author xBlackCat
 */
public class LocalTimeParser implements IParser<LocalTime> {
    @Override
    public Class<LocalTime> getReturnType() {
        return LocalTime.class;
    }

    @Override
    public LocalTime apply(String s) {
        return LocalTime.parse(s);
    }

    @Override
    public String formatDescription() {
        return "The formats accepted are based on the ISO-8601 extended local time format without offset, such as 10:15:30 or 10:15 or 10:15:30.44";
    }
}
