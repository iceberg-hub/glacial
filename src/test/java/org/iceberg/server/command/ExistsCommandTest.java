package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExistsCommandTest {

    private final Store store = new Store();
    private final ExistsCommand command = new ExistsCommand(store);

    @Test
    void returnsOneForExistingKey() {
        store.set("key", "value".getBytes());
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("EXISTS".getBytes()),
                new BulkString("key".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(1, ((RespInteger) result).value());
    }

    @Test
    void returnsZeroForNonExistentKey() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("EXISTS".getBytes()),
                new BulkString("missing".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(0, ((RespInteger) result).value());
    }

    @Test
    void countsMultipleExistingKeys() {
        store.set("a", "1".getBytes());
        store.set("b", "2".getBytes());
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("EXISTS".getBytes()),
                new BulkString("a".getBytes()),
                new BulkString("b".getBytes()),
                new BulkString("c".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(2, ((RespInteger) result).value());
    }

    @Test
    void errorWhenNoArguments() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("EXISTS".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenKeyIsNull() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("EXISTS".getBytes()),
                BulkString.NULL
        });
        assertInstanceOf(RespError.class, result);
    }
}
