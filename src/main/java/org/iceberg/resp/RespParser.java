package org.iceberg.resp;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RespParser {

    private static final byte[] OK_BYTES = "+OK\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_BULK = "$-1\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_ARRAY = "*-1\r\n".getBytes(StandardCharsets.UTF_8);

    public static RespValue parse(byte[] data) {
        return parse(new ByteArrayInputStream(data));
    }

    public static RespValue parse(InputStream in) {
        return parseValue(in);
    }

    public static byte[] serialize(RespValue value) {
        var baos = new ByteArrayOutputStream();
        serialize(value, baos);
        return baos.toByteArray();
    }

    public static void serialize(RespValue value, OutputStream out) {
        try {
            switch (value) {
                case SimpleString s -> writeSimpleString(s, out);
                case RespError e -> writeError(e, out);
                case RespInteger i -> writeInteger(i, out);
                case BulkString b -> writeBulkString(b, out);
                case Array a -> writeArray(a, out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean isOk(RespValue value) {
        return value instanceof SimpleString s && "OK".equals(s.value());
    }

    public static void serializeOk(OutputStream out) throws IOException {
        out.write(OK_BYTES);
    }

    private static RespValue parseValue(InputStream in) {
        try {
            int first = in.read();
            if (first == -1) {
                throw new IllegalArgumentException("Unexpected end of stream");
            }
            return switch ((char) first) {
                case '+' -> parseSimpleString(in);
                case '-' -> parseError(in);
                case ':' -> parseInteger(in);
                case '$' -> parseBulkString(in);
                case '*' -> parseArray(in);
                default -> throw new IllegalArgumentException(
                        "Unknown RESP type: '" + (char) first + "'");
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SimpleString parseSimpleString(InputStream in) throws IOException {
        return new SimpleString(readLine(in));
    }

    private static RespError parseError(InputStream in) throws IOException {
        return new RespError(readLine(in));
    }

    private static RespInteger parseInteger(InputStream in) throws IOException {
        var line = readLine(in);
        try {
            return new RespInteger(Long.parseLong(line));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer: " + line);
        }
    }

    private static BulkString parseBulkString(InputStream in) throws IOException {
        var line = readLine(in);
        int length = Integer.parseInt(line);
        if (length == -1) {
            return BulkString.NULL;
        }
        var buf = new byte[length];
        readFully(in, buf);
        skipCrLf(in);
        return new BulkString(buf);
    }

    private static Array parseArray(InputStream in) throws IOException {
        var line = readLine(in);
        int count = Integer.parseInt(line);
        if (count == -1) {
            return Array.NULL;
        }
        var items = new RespValue[count];
        for (int i = 0; i < count; i++) {
            items[i] = parseValue(in);
        }
        return new Array(items);
    }

    private static void writeSimpleString(SimpleString s, OutputStream out) throws IOException {
        out.write('+');
        out.write(s.value().getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
    }

    private static void writeError(RespError e, OutputStream out) throws IOException {
        out.write('-');
        out.write(e.value().getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
    }

    private static void writeInteger(RespInteger i, OutputStream out) throws IOException {
        out.write(':');
        writeInt(out, i.value());
        out.write(CRLF);
    }

    private static void writeBulkString(BulkString b, OutputStream out) throws IOException {
        if (b.value() == null) {
            out.write(NULL_BULK);
            return;
        }
        out.write('$');
        writeInt(out, b.value().length);
        out.write(CRLF);
        out.write(b.value());
        out.write(CRLF);
    }

    private static void writeArray(Array a, OutputStream out) throws IOException {
        if (a.value() == null) {
            out.write(NULL_ARRAY);
            return;
        }
        out.write('*');
        writeInt(out, a.value().length);
        out.write(CRLF);
        for (var item : a.value()) {
            serialize(item, out);
        }
    }

    private static final ThreadLocal<byte[]> LINE_BUF = ThreadLocal.withInitial(() -> new byte[64]);
    private static final ThreadLocal<byte[]> INT_BUF = ThreadLocal.withInitial(() -> new byte[21]);

    private static void writeInt(OutputStream out, long value) throws IOException {
        byte[] buf = INT_BUF.get();
        if (value == 0) {
            out.write('0');
            return;
        }
        int pos = buf.length;
        long v = value;
        if (v < 0) {
            if (v == Long.MIN_VALUE) {
                byte[] minVal = "-9223372036854775808".getBytes(StandardCharsets.UTF_8);
                out.write(minVal);
                return;
            }
            v = -v;
        }
        while (v > 0) {
            buf[--pos] = (byte) ('0' + (v % 10));
            v /= 10;
        }
        if (value < 0) {
            buf[--pos] = '-';
        }
        out.write(buf, pos, buf.length - pos);
    }

    private static String readLine(InputStream in) throws IOException {
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

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int offset = 0;
        while (offset < buf.length) {
            int read = in.read(buf, offset, buf.length - offset);
            if (read == -1) {
                throw new IllegalArgumentException("Unexpected end of stream");
            }
            offset += read;
        }
    }

    private static void skipCrLf(InputStream in) throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr != '\r' || lf != '\n') {
            throw new IllegalArgumentException("Expected CRLF");
        }
    }
}
