package org.xblackcat.sjpu.settings.converter;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Period;

/**
 * 02.02.2018 9:57
 *
 * @author xBlackCat
 */
public class TemporalAmountParserTest {
    @Test
    public void correctFormats() {
        final TemporalAmountParser parser = new TemporalAmountParser();
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("PT10.0001S"));
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("T10.0001S"));
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("10.0001S"));
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("10.0001"));

        Assert.assertEquals(Period.ofDays(10), parser.apply("P10D"));
        Assert.assertEquals(Period.ofDays(10), parser.apply("10D"));

        Assert.assertEquals(Period.ofYears(10), parser.apply("P10Y"));
        Assert.assertEquals(Period.ofYears(10), parser.apply("10Y"));

        Assert.assertEquals(Period.ofMonths(10), parser.apply("P10M"));
        Assert.assertEquals(Period.ofMonths(10), parser.apply("10M"));

        Assert.assertEquals(Duration.ofMinutes(10), parser.apply("PT10M"));
        Assert.assertEquals(Duration.ofMinutes(10), parser.apply("T10M"));

        Assert.assertEquals(Duration.ofDays(10).plus(Duration.ofHours(1)), parser.apply("P10DT1H"));
        Assert.assertEquals(Duration.ofDays(10).plus(Duration.ofHours(1)), parser.apply("10DT1H"));
    }

    @Test
    public void illegalFormats() {
        final TemporalAmountParser parser = new TemporalAmountParser();
        try {
            parser.apply("P10Y2MT1H");
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}