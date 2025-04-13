package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ByteSize  implements Token{
    private final long bytes;
    private final String raw;

    public ByteSize(String raw) {
        this.raw = raw;
        this.bytes = parseBytes(raw.trim().toUpperCase());
    }

    private long parseBytes(String value) {
        if (value.endsWith("KB")) return (long) (Double.parseDouble(value.replace("KB", "")) * 1024);
        if (value.endsWith("MB")) return (long) (Double.parseDouble(value.replace("MB", "")) * 1024 * 1024);
        if (value.endsWith("GB")) return (long) (Double.parseDouble(value.replace("GB", "")) * 1024 * 1024 * 1024);
        if (value.endsWith("B")) return Long.parseLong(value.replace("B", ""));
        return Long.parseLong(value); // assume raw bytes
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public Object value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(bytes);
    }
}
