package org.xblackcat.sjpu.settings.converter;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;

/**
 * 31.08.2016 10:00
 *
 * @author xBlackCat
 */
public class TemporalAmountParser implements IParser<TemporalAmount> {
    @Override
    public Class<TemporalAmount> getReturnType() {
        return TemporalAmount.class;
    }

    @Override
    public TemporalAmount apply(String s) {
        DateTimeParseException cachedException;
        try {
            return Period.parse(s);
        } catch (DateTimeParseException e) {
            cachedException = e;
        }

        try {
            return Duration.parse(s);
        } catch (DateTimeParseException e) {
            // ignore - try other formats
        }

        try {
            return Period.parse("P" + s);
        } catch (DateTimeParseException e) {
            // Ignore - try other formats
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
        return "The formats accepted are based on the ISO-8601 period formats PnYnMnD and PnW or ISO-8601 duration format PnDTnHnMn.nS. " +
                "Possible to specify the string without 'P' prefix or as number. In the last case the number will be treated as seconds amount. " +
                "To specify 'minutes' always use T<amount>M format. <amount>M will be treated as 'months'";
    }
}
