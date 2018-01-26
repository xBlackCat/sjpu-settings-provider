package org.xblackcat.sjpu.settings.converter;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * 31.08.2016 10:00
 *
 * @author xBlackCat
 */
public class DurationParser implements IParser<Duration> {
    @Override
    public Class<Duration> getReturnType() {
        return Duration.class;
    }

    @Override
    public Duration apply(String s) {
        DateTimeParseException cachedException;
        try {
            return Duration.parse(s);
        } catch (DateTimeParseException e) {
            cachedException = e;
        }

        try {
            return Duration.parse("P" + s);
        } catch (DateTimeParseException e) {
            // Ignore - try other formats
        }

        try {
            return Duration.parse("PT" + s);
        } catch (DateTimeParseException e) {
            // Ignore - try other formats
        }

        try {
            return Duration.parse("PT" + s + "S");
        } catch (DateTimeParseException e) {
            // Ignore - try other formats
        }

        throw cachedException;
    }

    @Override
    public String formatDescription() {
        return "The formats accepted are based on the ISO-8601 duration format PnDTnHnMn.nS. Possible to specify the " +
                "string without 'P' prefix or as number. In the last case the number will be treated as seconds amount";
    }
}
