package org.iceberg.resp;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RespDecoder {

    private static final ThreadLocal<byte[]> LINE_BUF = ThreadLocal.withInitial(() -> new byte[64]);

    private RespDecoder() {}

    public static RespValue decode(InputStream in) {
        try {
            return readValue(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static RespValue readValue(InputStream in) throws IOException {
        int typeByte = in.read();
        if (typeByte == -1) {
            throw new IllegalArgumentException("Unexpected end of stream");
        }
        return switch ((char) typeByte) {
            case '+' -> readSimpleString(in);
            case '-' -> readError(in);
            case ':' -> readInteger(in);
            case '$' -> readBulkString(in);
            case '*' -> readArray(in);
            default -> throw new IllegalArgumentException(
                    "Unknown RESP type: '" + (char) typeByte + "'");
        };
    }

    private static SimpleString readSimpleString(InputStream in) throws IOException {
        return new SimpleString(readLine(in));
    }

    private static RespError readError(InputStream in) throws IOException {
        return new RespError(readLine(in));
    }

    private static RespInteger readInteger(InputStream in) throws IOException {
        var line = readLine(in);
        try {
            return new RespInteger(Long.parseLong(line));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer: " + line);
        }
    }

    private static BulkString readBulkString(InputStream in) throws IOException {
        var line = readLine(in);
        int length = Integer.parseInt(line);
        if (length == -1) {
            return BulkString.NULL;
        }
        var buf = new byte[length];
        readFully(in, buf);
        expectCrLf(in);
        return new BulkString(buf);
    }

    private static Array readArray(InputStream in) throws IOException {
        var line = readLine(in);
        int count = Integer.parseInt(line);
        if (count == -1) {
            return Array.NULL;
        }
        var items = new RespValue[count];
        for (int i = 0; i < count; i++) {
            items[i] = readValue(in);
        }
        return new Array(items);
    }

    static String readLine(InputStream in) throws IOException {
        byte[] buf = LINE_BUF.get();
        int len = 0;
        int prev = in.read();
        if (prev == -1) {
            throw new IllegalArgumentException("Unexpected end of stream");
        }
        while (true) {
            int cur = in.read();
            if (cur == -1) {
                throw new IllegalArgumentException("Unexpected end of stream");
            }
            if (prev == '\r' && cur == '\n') {
                break;
            }
            if (len == buf.length) {
                buf = java.util.Arrays.copyOf(buf, buf.length * 2);
                LINE_BUF.set(buf);
            }
            buf[len++] = (byte) prev;
            prev = cur;
        }
        return new String(buf, 0, len, StandardCharsets.UTF_8);
    }

    static void readFully(InputStream in, byte[] buf) throws IOException {
        int offset = 0;
        while (offset < buf.length) {
            int read = in.read(buf, offset, buf.length - offset);
            if (read == -1) {
                throw new IllegalArgumentException("Unexpected end of stream");
            }
            offset += read;
        }
    }

    private static void expectCrLf(InputStream in) throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr != '\r' || lf != '\n') {
            throw new IllegalArgumentException("Expected CRLF");
        }
    }
}
