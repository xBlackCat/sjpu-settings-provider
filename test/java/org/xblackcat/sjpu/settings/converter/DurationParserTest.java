package org.xblackcat.sjpu.settings.converter;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

/**
 * 02.02.2018 9:57
 *
 * @author xBlackCat
 */
public class DurationParserTest {
    @Test
    public void correctFormats() {
        final DurationParser parser = new DurationParser();
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("PT10.0001S"));
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("T10.0001S"));
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("10.0001S"));
        Assert.assertEquals(Duration.ofSeconds(10,100000), parser.apply("10.0001"));

        Assert.assertEquals(Duration.ofMinutes(10), parser.apply("PT10M"));
        Assert.assertEquals(Duration.ofMinutes(10), parser.apply("T10M"));
        Assert.assertEquals(Duration.ofMinutes(10), parser.apply("10M"));

        Assert.assertEquals(Duration.ofDays(10), parser.apply("P10D"));
        Assert.assertEquals(Duration.ofDays(10), parser.apply("10D"));
    }

    @Test
    public void illegalFormats() {
        final DurationParser parser = new DurationParser();
        try {
            parser.apply("PT10D");
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}