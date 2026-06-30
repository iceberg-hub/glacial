package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.SimpleString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EchoCommandTest {

    private final EchoCommand command = new EchoCommand();

    @Test
    void echoesBulkString() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("ECHO".getBytes()),
                new BulkString("Hello World".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(BulkString.class, result);
        assertArrayEquals("Hello World".getBytes(), ((BulkString) result).value());
    }

    @Test
    void echoesInteger() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("ECHO".getBytes()),
                new org.iceberg.resp.RespInteger(42)
        };
        var result = command.execute(args);
        assertInstanceOf(org.iceberg.resp.RespInteger.class, result);
        assertEquals(42, ((org.iceberg.resp.RespInteger) result).value());
    }

    @Test
    void echoesSimpleString() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("ECHO".getBytes()),
                new SimpleString("OK")
        };
        var result = command.execute(args);
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
    }

    @Test
    void errorWhenNoArgument() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("ECHO".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }
}
