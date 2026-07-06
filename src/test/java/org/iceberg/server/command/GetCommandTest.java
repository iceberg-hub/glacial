package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetCommandTest {

    private final Store store = new Store();
    private final GetCommand command = new GetCommand(store);

    @Test
    void getsExistingValue() {
        store.set("Name", "John".getBytes());
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("GET".getBytes()),
                new BulkString("Name".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(BulkString.class, result);
        assertArrayEquals("John".getBytes(), ((BulkString) result).value());
    }

    @Test
    void returnsNullForNonExistentKey() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("GET".getBytes()),
                new BulkString("NonExistent".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(BulkString.class, result);
        assertNull(((BulkString) result).value());
    }

    @Test
    void errorWhenNoArguments() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("GET".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenTooManyArguments() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("GET".getBytes()),
                new BulkString("key".getBytes()),
                new BulkString("extra".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenKeyIsNull() {
        var args = new org.iceberg.resp.RespValue[]{
                new BulkString("GET".getBytes()),
                BulkString.NULL
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
    }
}
