package org.iceberg.resp;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RespEncoder {

    private static final byte[] OK_BYTES = "+OK\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_BULK = "$-1\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_ARRAY = "*-1\r\n".getBytes(StandardCharsets.UTF_8);

    private static final ThreadLocal<byte[]> INT_BUF = ThreadLocal.withInitial(() -> new byte[21]);

    private RespEncoder() {}

    public static byte[] encode(RespValue value) {
        var baos = new ByteArrayOutputStream();
        encode(value, baos);
        return baos.toByteArray();
    }

    public static void encode(RespValue value, OutputStream out) {
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

    public static void encodeOk(OutputStream out) throws IOException {
        out.write(OK_BYTES);
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
            encode(item, out);
        }
    }

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
                out.write("-9223372036854775808".getBytes(StandardCharsets.UTF_8));
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
}
