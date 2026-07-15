package org.iceberg.resp;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RespEncoderTest {

    @Test
    void encodeSimpleString() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new SimpleString("OK"), out);
        assertEquals("+OK\r\n", out.toString());
    }

    @Test
    void encodeError() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new RespError("ERR bad"), out);
        assertEquals("-ERR bad\r\n", out.toString());
    }

    @Test
    void encodeInteger() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new RespInteger(42), out);
        assertEquals(":42\r\n", out.toString());
    }

    @Test
    void encodeNegativeInteger() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new RespInteger(-1), out);
        assertEquals(":-1\r\n", out.toString());
    }

    @Test
    void encodeBulkString() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new BulkString("hello".getBytes()), out);
        assertEquals("$5\r\nhello\r\n", out.toString());
    }

    @Test
    void encodeNullBulkString() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(BulkString.NULL, out);
        assertEquals("$-1\r\n", out.toString());
    }

    @Test
    void encodeEmptyBulkString() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new BulkString(new byte[0]), out);
        assertEquals("$0\r\n\r\n", out.toString());
    }

    @Test
    void encodeArray() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(new Array(new RespValue[]{
            new BulkString("GET".getBytes()),
            new BulkString("key".getBytes())
        }), out);
        assertEquals("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n", out.toString());
    }

    @Test
    void encodeNullArray() {
        var out = new ByteArrayOutputStream();
        RespEncoder.encode(Array.NULL, out);
        assertEquals("*-1\r\n", out.toString());
    }

    @Test
    void encodeReturnsByteArray() {
        var bytes = RespEncoder.encode(new SimpleString("OK"));
        assertEquals("+OK\r\n", new String(bytes));
    }

    @Test
    void encodeOkWritesPrecomputedBytes() throws Exception {
        var out = new ByteArrayOutputStream();
        RespEncoder.encodeOk(out);
        assertEquals("+OK\r\n", out.toString());
    }
}
