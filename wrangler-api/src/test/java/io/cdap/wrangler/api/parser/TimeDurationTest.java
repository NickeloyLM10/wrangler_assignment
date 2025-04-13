package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the TimeDuration class.
 * These tests verify the correct parsing and handling of time duration values with different units.
 */
public class TimeDurationTest {

    @Test
    public void testValidTimeDurations() {
        // Test various time duration formats
        Assert.assertEquals(150L, new TimeDuration("150ms").value().longValue());
        Assert.assertEquals(150L, new TimeDuration("150MS").value().longValue());
        Assert.assertEquals(2500L, new TimeDuration("2.5s").value().longValue());
        Assert.assertEquals(180000L, new TimeDuration("3m").value().longValue());
        Assert.assertEquals(3600000L, new TimeDuration("1h").value().longValue());
        Assert.assertEquals(5400000L, new TimeDuration("1.5h").value().longValue());
    }

    @Test
    public void testDecimalValues() {
        // Test decimal values in different units
        Assert.assertEquals(2500L, new TimeDuration("2.5s").value().longValue());
        Assert.assertEquals(90000L, new TimeDuration("1.5m").value().longValue());
        Assert.assertEquals(5400000L, new TimeDuration("1.5h").value().longValue());
    }

    @Test
    public void testCaseInsensitivity() {
        // Test case insensitivity of units
        Assert.assertEquals(150L, new TimeDuration("150ms").value().longValue());
        Assert.assertEquals(150L, new TimeDuration("150MS").value().longValue());
        Assert.assertEquals(150L, new TimeDuration("150mS").value().longValue());
        Assert.assertEquals(150L, new TimeDuration("150Ms").value().longValue());
    }

    @Test
    public void testRawMilliseconds() {
        // Test raw millisecond values (without units)
        Assert.assertEquals(100L, new TimeDuration("100").value().longValue());
        Assert.assertEquals(0L, new TimeDuration("0").value().longValue());
        Assert.assertEquals(1L, new TimeDuration("1").value().longValue());
    }

    @Test
    public void testGetOriginalValue() {
        // Test the getOriginalValue method
        TimeDuration duration = new TimeDuration("150ms");
        Assert.assertEquals("150ms", duration.getOriginalValue());

        TimeDuration durationWithSpaces = new TimeDuration(" 2.5s ");
        Assert.assertEquals(" 2.5s ", durationWithSpaces.getOriginalValue());
    }

    @Test
    public void testTokenType() {
        // Test token type
        TimeDuration duration = new TimeDuration("150ms");
        Assert.assertEquals(TokenType.TIME_DURATION, duration.type());
    }

    @Test
    public void testToJson() {
        // Test JSON serialization
        TimeDuration duration = new TimeDuration("150ms");
        JsonElement json = duration.toJson();

        Assert.assertTrue(json.isJsonObject());
        JsonObject jsonObject = json.getAsJsonObject();

        Assert.assertTrue(jsonObject.has("type"));
        Assert.assertTrue(jsonObject.has("value"));
        Assert.assertEquals(TokenType.TIME_DURATION.name(), jsonObject.get("type").getAsString());
        Assert.assertEquals(150L, jsonObject.get("value").getAsLong());
    }

    @Test(expected = NumberFormatException.class)
    public void testInvalidNumericFormat() {
        // Test invalid numeric format
        new TimeDuration("abc").value();
    }

    @Test
    public void testWhitespaceHandling() {
        // Test whitespace handling
        Assert.assertEquals(150L, new TimeDuration(" 150ms").value().longValue());
        Assert.assertEquals(150L, new TimeDuration("150ms ").value().longValue());
        Assert.assertEquals(150L, new TimeDuration(" 150ms ").value().longValue());
    }

    @Test
    public void testLargeValues() {
        // Test large values
        Assert.assertEquals(3600000000L, new TimeDuration("1000h").value().longValue());
        Assert.assertEquals(86400000L, new TimeDuration("24h").value().longValue()); // 1 day
    }

    @Test
    public void testZeroValues() {
        // Test zero values with different units
        Assert.assertEquals(0L, new TimeDuration("0ms").value().longValue());
        Assert.assertEquals(0L, new TimeDuration("0s").value().longValue());
        Assert.assertEquals(0L, new TimeDuration("0m").value().longValue());
        Assert.assertEquals(0L, new TimeDuration("0h").value().longValue());
    }

    @Test
    public void testSmallDecimalValues() {
        // Test small decimal values
        Assert.assertEquals(1L, new TimeDuration("0.001s").value().longValue());
        Assert.assertEquals(10L, new TimeDuration("0.01s").value().longValue());
        Assert.assertEquals(100L, new TimeDuration("0.1s").value().longValue());
    }

    @Test
    public void testBoundaryValues() {
        // Test boundary values
        Assert.assertEquals(Long.MAX_VALUE, new TimeDuration(String.valueOf(Long.MAX_VALUE)).value().longValue());
        Assert.assertEquals(Long.MIN_VALUE + 1, new TimeDuration(String.valueOf(Long.MIN_VALUE + 1)).value().longValue());
    }
}
