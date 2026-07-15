package org.iceberg.resp;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class RespDecoderTest {

    @Test
    void decodeSimpleString() {
        var in = new ByteArrayInputStream("+OK\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
    }

    @Test
    void decodeError() {
        var in = new ByteArrayInputStream("-ERR bad\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertInstanceOf(RespError.class, result);
        assertEquals("ERR bad", ((RespError) result).value());
    }

    @Test
    void decodeInteger() {
        var in = new ByteArrayInputStream(":42\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertInstanceOf(RespInteger.class, result);
        assertEquals(42, ((RespInteger) result).value());
    }

    @Test
    void decodeBulkString() {
        var in = new ByteArrayInputStream("$5\r\nhello\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertInstanceOf(BulkString.class, result);
        assertArrayEquals("hello".getBytes(), ((BulkString) result).value());
    }

    @Test
    void decodeNullBulkString() {
        var in = new ByteArrayInputStream("$-1\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertEquals(BulkString.NULL, result);
    }

    @Test
    void decodeArray() {
        var in = new ByteArrayInputStream("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertInstanceOf(Array.class, result);
        var arr = (Array) result;
        assertEquals(2, arr.value().length);
    }

    @Test
    void decodeNullArray() {
        var in = new ByteArrayInputStream("*-1\r\n".getBytes());
        var result = RespDecoder.decode(in);
        assertEquals(Array.NULL, result);
    }

    @Test
    void decodeEmptyStreamThrows() {
        var in = new ByteArrayInputStream(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> RespDecoder.decode(in));
    }
}
