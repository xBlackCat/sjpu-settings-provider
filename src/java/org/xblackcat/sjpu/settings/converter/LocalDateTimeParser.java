package org.xblackcat.sjpu.settings.converter;

import java.time.LocalDateTime;

/**
 * 31.08.2016 10:00
 *
 * @author xBlackCat
 */
public class LocalDateTimeParser implements IParser<LocalDateTime> {
    @Override
    public Class<LocalDateTime> getReturnType() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime apply(String s) {
        return LocalDateTime.parse(s);
    }

    @Override
    public String formatDescription() {
        return "The formats accepted are based on the ISO-8601 date/time format , such as '2007-12-03T10:15:30'";
    }
}
