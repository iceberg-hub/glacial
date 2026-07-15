package org.iceberg.resp;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RespParser {

    private RespParser() {}

    public static RespValue parse(byte[] data) {
        return parse(new ByteArrayInputStream(data));
    }

    public static RespValue parse(InputStream in) {
        return RespDecoder.decode(in);
    }

    public static byte[] serialize(RespValue value) {
        return RespEncoder.encode(value);
    }

    public static void serialize(RespValue value, OutputStream out) {
        RespEncoder.encode(value, out);
    }

    public static boolean isOk(RespValue value) {
        return value instanceof SimpleString s && "OK".equals(s.value());
    }

    public static void serializeOk(OutputStream out) throws IOException {
        RespEncoder.encodeOk(out);
    }
}
