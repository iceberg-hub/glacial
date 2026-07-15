package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelCommandTest {

    private final Store store = new Store();
    private final DelCommand command = new DelCommand(store);

    @Test
    void deletesExistingKey() {
        store.set("key", "value".getBytes());
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("DEL".getBytes()),
                new BulkString("key".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(1, ((RespInteger) result).value());
        assertNull(store.get("key"));
    }

    @Test
    void returnsZeroForNonExistentKey() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("DEL".getBytes()),
                new BulkString("missing".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(0, ((RespInteger) result).value());
    }

    @Test
    void deletesMultipleKeys() {
        store.set("a", "1".getBytes());
        store.set("b", "2".getBytes());
        store.set("c", "3".getBytes());
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("DEL".getBytes()),
                new BulkString("a".getBytes()),
                new BulkString("b".getBytes()),
                new BulkString("x".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(2, ((RespInteger) result).value());
        assertNull(store.get("a"));
        assertNull(store.get("b"));
        assertNotNull(store.get("c"));
    }

    @Test
    void errorWhenNoArguments() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("DEL".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenKeyIsNull() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("DEL".getBytes()),
                BulkString.NULL
        });
        assertInstanceOf(RespError.class, result);
    }
}
