package org.iceberg.server.async;

import org.iceberg.resp.*;

public class RespDecoder {

    public record ParseResult(RespValue value, int consumed) {}

    public ParseResult tryParseOne(byte[] data, int offset) {
        var remaining = data.length - offset;
        if (remaining < 1) return null;

        return switch ((char) data[offset]) {
            case '+' -> parseSimpleString(data, offset);
            case '-' -> parseError(data, offset);
            case ':' -> parseInteger(data, offset);
            case '$' -> parseBulkString(data, offset);
            case '*' -> parseArray(data, offset);
            default -> throw new IllegalArgumentException(
                "Unknown RESP type: '" + (char) data[offset] + "'");
        };
    }

    public ParseResult parseSimpleString(byte[] data, int offset) {
        var end = findCrLf(data, offset + 1);
        if (end == -1) return null;
        var str = new String(data, offset + 1, end - offset - 1);
        return new ParseResult(new SimpleString(str), end + 2 - offset);
    }

    public ParseResult parseError(byte[] data, int offset) {
        var end = findCrLf(data, offset + 1);
        if (end == -1) return null;
        var str = new String(data, offset + 1, end - offset - 1);
        return new ParseResult(new RespError(str), end + 2 - offset);
    }

    public ParseResult parseInteger(byte[] data, int offset) {
        var end = findCrLf(data, offset + 1);
        if (end == -1) return null;
        var str = new String(data, offset + 1, end - offset - 1);
        return new ParseResult(new RespInteger(Long.parseLong(str)), end + 2 - offset);
    }

    public ParseResult parseBulkString(byte[] data, int offset) {
        var end = findCrLf(data, offset + 1);
        if (end == -1) return null;
        var lenStr = new String(data, offset + 1, end - offset - 1);
        var length = Integer.parseInt(lenStr);
        if (length == -1) {
            return new ParseResult(BulkString.NULL, end + 2 - offset);
        }
        var valueStart = end + 2;
        var valueEnd = valueStart + length;
        if (valueEnd + 2 > data.length) return null;
        if (data[valueEnd] != '\r' || data[valueEnd + 1] != '\n') return null;
        var value = new byte[length];
        System.arraycopy(data, valueStart, value, 0, length);
        return new ParseResult(new BulkString(value), valueEnd + 2 - offset);
    }

    public ParseResult parseArray(byte[] data, int offset) {
        var end = findCrLf(data, offset + 1);
        if (end == -1) return null;
        var countStr = new String(data, offset + 1, end - offset - 1);
        var count = Integer.parseInt(countStr);
        if (count == -1) {
            return new ParseResult(Array.NULL, end + 2 - offset);
        }
        var items = new RespValue[count];
        var currentOffset = end + 2;
        for (var i = 0; i < count; i++) {
            var result = tryParseOne(data, currentOffset);
            if (result == null) return null;
            items[i] = result.value();
            currentOffset += result.consumed();
        }
        return new ParseResult(new Array(items), currentOffset - offset);
    }

    public int findCrLf(byte[] data, int start) {
        for (var i = start; i < data.length - 1; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n') {
                return i;
            }
        }
        return -1;
    }
}
