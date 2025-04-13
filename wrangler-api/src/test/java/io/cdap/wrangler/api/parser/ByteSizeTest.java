package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the ByteSize class.
 * These tests verify the correct parsing and handling of byte size values with different units.
 */
public class ByteSizeTest {

    @Test
    public void testValidByteSizes() {
        // Test various byte size formats
        Assert.assertEquals(10240L, new ByteSize("10KB").value().longValue());
        Assert.assertEquals(10240L, new ByteSize("10kb").value().longValue());
        Assert.assertEquals(1572864L, new ByteSize("1.5MB").value().longValue());
        Assert.assertEquals(5368709120L, new ByteSize("5GB").value().longValue());
        Assert.assertEquals(2199023255552L, new ByteSize("2TB").value().longValue());
        Assert.assertEquals(100L, new ByteSize("100B").value().longValue());
        Assert.assertEquals(1024L, new ByteSize("1KB").value().longValue());
    }

    @Test
    public void testDecimalValues() {
        // Test decimal values in different units
        Assert.assertEquals(1536L, new ByteSize("1.5KB").value().longValue());
        Assert.assertEquals(2097152L, new ByteSize("2.0MB").value().longValue());
        Assert.assertEquals(3221225472L, new ByteSize("3.0GB").value().longValue());
        Assert.assertEquals(1099511627776L, new ByteSize("1.0TB").value().longValue());
    }

    @Test
    public void testCaseInsensitivity() {
        // Test case insensitivity of units
        Assert.assertEquals(1024L, new ByteSize("1kb").value().longValue());
        Assert.assertEquals(1024L, new ByteSize("1KB").value().longValue());
        Assert.assertEquals(1024L, new ByteSize("1Kb").value().longValue());
        Assert.assertEquals(1024L, new ByteSize("1kB").value().longValue());
    }

    @Test
    public void testRawBytes() {
        // Test raw byte values (without units)
        Assert.assertEquals(100L, new ByteSize("100").value().longValue());
        Assert.assertEquals(0L, new ByteSize("0").value().longValue());
        Assert.assertEquals(1L, new ByteSize("1").value().longValue());
    }

    @Test
    public void testGetOriginalValue() {
        // Test the getOriginalValue method
        ByteSize byteSize = new ByteSize("10KB");
        Assert.assertEquals("10KB", byteSize.getOriginalValue());

        ByteSize byteSizeWithSpaces = new ByteSize(" 5MB ");
        Assert.assertEquals(" 5MB ", byteSizeWithSpaces.getOriginalValue());
    }

    @Test
    public void testTokenType() {
        // Test token type
        ByteSize byteSize = new ByteSize("10KB");
        Assert.assertEquals(TokenType.BYTE_SIZE, byteSize.type());
    }

    @Test
    public void testToJson() {
        // Test JSON serialization
        ByteSize byteSize = new ByteSize("10KB");
        JsonElement json = byteSize.toJson();

        Assert.assertTrue(json.isJsonObject());
        JsonObject jsonObject = json.getAsJsonObject();

        Assert.assertTrue(jsonObject.has("type"));
        Assert.assertTrue(jsonObject.has("value"));
        Assert.assertEquals(TokenType.BYTE_SIZE.name(), jsonObject.get("type").getAsString());
        Assert.assertEquals(10240L, jsonObject.get("value").getAsLong());
    }

    @Test(expected = NumberFormatException.class)
    public void testInvalidNumericFormat() {
        // Test invalid numeric format
        new ByteSize("abc").value();
    }

    @Test
    public void testWhitespaceHandling() {
        // Test whitespace handling
        Assert.assertEquals(1024L, new ByteSize(" 1KB").value().longValue());
        Assert.assertEquals(1024L, new ByteSize("1KB ").value().longValue());
        Assert.assertEquals(1024L, new ByteSize(" 1KB ").value().longValue());
    }

    @Test
    public void testLargeValues() {
        // Test large values
        Assert.assertEquals(10995116277760L, new ByteSize("10TB").value().longValue());
        Assert.assertEquals(1099511627776000L, new ByteSize("1000TB").value().longValue());
    }

    @Test
    public void testZeroValues() {
        // Test zero values with different units
        Assert.assertEquals(0L, new ByteSize("0KB").value().longValue());
        Assert.assertEquals(0L, new ByteSize("0MB").value().longValue());
        Assert.assertEquals(0L, new ByteSize("0GB").value().longValue());
        Assert.assertEquals(0L, new ByteSize("0TB").value().longValue());
        Assert.assertEquals(0L, new ByteSize("0B").value().longValue());
    }
}
