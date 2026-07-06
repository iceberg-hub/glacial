package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.SimpleString;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetCommandTest {

    private final SetCommand command = new SetCommand(new Store());

    @Test
    void setsValue() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("SET".getBytes()),
                new BulkString("Name".getBytes()),
                new BulkString("John".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
    }

    @Test
    void errorWhenNoArguments() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("SET".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenMissingValue() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("SET".getBytes()),
                new BulkString("key".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenKeyIsNull() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("SET".getBytes()),
                BulkString.NULL,
                new BulkString("value".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
    }
}
