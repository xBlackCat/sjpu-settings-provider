package org.xblackcat.sjpu.settings.converter;

import java.time.LocalDate;

/**
 * 31.08.2016 10:00
 *
 * @author xBlackCat
 */
public class LocalDateParser implements IParser<LocalDate> {
    @Override
    public Class<LocalDate> getReturnType() {
        return LocalDate.class;
    }

    @Override
    public LocalDate apply(String s) {
        return LocalDate.parse(s);
    }

    @Override
    public String formatDescription() {
        return "The formats accepted are based on the ISO-8601 date format , such as '2011-12-03'";
    }
}
