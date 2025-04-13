package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Represents a time duration token in the CDAP Wrangler parser.
 * This class handles parsing and storing time duration values with their units.
 */
@PublicEvolving
public class TimeDuration implements Token {
    private final long milliseconds;
    private final String originalValue;

    public TimeDuration(String value) {
        this.originalValue = value;
        this.milliseconds = parseMilliseconds(value.trim().toLowerCase());
    }

    private long parseMilliseconds(String value) {
        if (value.endsWith("ms")) return Long.parseLong(value.replace("ms", ""));
        if (value.endsWith("s")) return (long) (Double.parseDouble(value.replace("s", "")) * 1000);
        if (value.endsWith("m")) return (long) (Double.parseDouble(value.replace("m", "")) * 60 * 1000);
        if (value.endsWith("h")) return (long) (Double.parseDouble(value.replace("h", "")) * 60 * 60 * 1000);
        return Long.parseLong(value); // assume raw milliseconds
    }

    @Override
    public Long value() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value", milliseconds);
        return object;
    }

    public String getOriginalValue() {
        return originalValue;
    }
}
