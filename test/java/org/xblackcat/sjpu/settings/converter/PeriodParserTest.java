package org.xblackcat.sjpu.settings.converter;

import org.junit.Assert;
import org.junit.Test;

import java.time.Period;

/**
 * 02.02.2018 9:57
 *
 * @author xBlackCat
 */
public class PeriodParserTest {
    @Test
    public void correctFormats() {
        final PeriodParser parser = new PeriodParser();
        Assert.assertEquals(Period.ofDays(10), parser.apply("P10D"));
        Assert.assertEquals(Period.ofDays(10), parser.apply("10D"));
        Assert.assertEquals(Period.ofDays(10), parser.apply("10"));

        Assert.assertEquals(Period.ofYears(10), parser.apply("P10Y"));
        Assert.assertEquals(Period.ofYears(10), parser.apply("10Y"));
    }

    @Test
    public void illegalFormats() {
        final PeriodParser parser = new PeriodParser();
        try {
            parser.apply("PT10Y");
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
        try {
            parser.apply("PT10M");
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}