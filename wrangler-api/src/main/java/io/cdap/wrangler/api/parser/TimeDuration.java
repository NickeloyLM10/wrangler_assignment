package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class TimeDuration implements Token {

    private final long millis;
    private final String raw;

    public TimeDuration(String raw) {
        this.raw = raw;
        this.millis = parseMillis(raw.trim().toUpperCase());
    }

    private long parseMillis(String value) {
        if (value.endsWith("ms")) return Long.parseLong(value.replace("ms", ""));
        if (value.endsWith("s")) return (long) (Double.parseDouble(value.replace("s", "")) * 1000);
        if (value.endsWith("m")) return (long) (Double.parseDouble(value.replace("m", "")) * 60 * 1000);
        if (value.endsWith("h")) return (long) (Double.parseDouble(value.replace("h", "")) * 60 * 60 * 1000);
        return Long.parseLong(value); // assume raw milliseconds
    }

    public long getMillis() {
        return millis;
    }

    @Override
    public Object value() {
        return millis;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(millis);
    }
}
