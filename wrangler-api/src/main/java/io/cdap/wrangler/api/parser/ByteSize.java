package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Represents a byte size token in the CDAP Wrangler parser.
 * This class handles parsing and storing byte size values with their units.
 */
@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;
    private final String originalValue;

    public ByteSize(String value) {
        this.originalValue = value;
        this.bytes = parseBytes(value.trim().toUpperCase());
    }

    private long parseBytes(String value) {
        if (value.endsWith("KB")) return (long) (Double.parseDouble(value.replace("KB", "")) * 1024);
        if (value.endsWith("MB")) return (long) (Double.parseDouble(value.replace("MB", "")) * 1024 * 1024);
        if (value.endsWith("GB")) return (long) (Double.parseDouble(value.replace("GB", "")) * 1024 * 1024 * 1024);
        if (value.endsWith("TB")) return (long) (Double.parseDouble(value.replace("TB", "")) * 1024 * 1024 * 1024 * 1024);
        if (value.endsWith("B")) return Long.parseLong(value.replace("B", ""));
        return Long.parseLong(value); // assume raw bytes
    }

    @Override
    public Long value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", bytes);
        return object;
    }

    public String getOriginalValue() {
        return originalValue;
    }
}
