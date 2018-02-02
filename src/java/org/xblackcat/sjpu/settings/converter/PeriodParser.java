package org.xblackcat.sjpu.settings.converter;

import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * 31.08.2016 10:00
 *
 * @author xBlackCat
 */
public class PeriodParser implements IParser<Period> {
    @Override
    public Class<Period> getReturnType() {
        return Period.class;
    }

    @Override
    public Period apply(String s) {
        DateTimeParseException cachedException;
        try {
            return Period.parse(s);
        } catch (DateTimeParseException e) {
            cachedException = e;
        }

        try {
            return Period.parse("P" + s);
        } catch (DateTimeParseException e) {
            // Ignore - try other formats
        }
        try {
            return Period.parse("P" + s + "D");
        } catch (DateTimeParseException e) {
            // Ignore - try other formats
        }

        throw cachedException;
    }

    @Override
    public String formatDescription() {
        return "The formats accepted are based on the ISO-8601 period formats PnYnMnD and PnW. " +
                "Possible to specify the string without 'P' prefix or as number. In the last case the number will be treated as days amount";
    }
}
