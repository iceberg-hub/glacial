package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LpushCommandTest {

    private final Store store = new Store();
    private final LpushCommand command = new LpushCommand(store);

    @Test
    void lpushSingleValueToNewList() {
        var result = command.execute(new RespValue[]{
                new BulkString("LPUSH".getBytes()),
                new BulkString("mylist".getBytes()),
                new BulkString("a".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(1, ((RespInteger) result).value());
        var list = store.lrange("mylist", 0, -1);
        assertEquals(1, list.size());
        assertArrayEquals("a".getBytes(), list.get(0));
    }

    @Test
    void lpushMultipleValues() {
        var result = command.execute(new RespValue[]{
                new BulkString("LPUSH".getBytes()),
                new BulkString("mylist".getBytes()),
                new BulkString("a".getBytes()),
                new BulkString("b".getBytes()),
                new BulkString("c".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(3, ((RespInteger) result).value());
        var list = store.lrange("mylist", 0, -1);
        assertEquals(3, list.size());
        assertArrayEquals("c".getBytes(), list.get(0));
        assertArrayEquals("b".getBytes(), list.get(1));
        assertArrayEquals("a".getBytes(), list.get(2));
    }

    @Test
    void lpushToExistingList() {
        store.lpush("mylist", "a".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("LPUSH".getBytes()),
                new BulkString("mylist".getBytes()),
                new BulkString("b".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(2, ((RespInteger) result).value());
        var list = store.lrange("mylist", 0, -1);
        assertArrayEquals("b".getBytes(), list.get(0));
        assertArrayEquals("a".getBytes(), list.get(1));
    }

    @Test
    void errorWhenTooFewArgs() {
        var result = command.execute(new RespValue[]{
                new BulkString("LPUSH".getBytes()),
                new BulkString("mylist".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenKeyIsNonListType() {
        store.set("strkey", "value".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("LPUSH".getBytes()),
                new BulkString("strkey".getBytes()),
                new BulkString("a".getBytes())
        });
        assertInstanceOf(RespError.class, result);
    }
}
