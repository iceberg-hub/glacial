package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecrCommandTest {

    private final Store store = new Store();
    private final DecrCommand command = new DecrCommand(store);

    @Test
    void decrNonExistentKeyReturnsMinusOne() {
        var result = command.execute(new RespValue[]{
                new BulkString("DECR".getBytes()),
                new BulkString("counter".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(-1, ((RespInteger) result).value());
    }

    @Test
    void decrExistingInteger() {
        store.set("counter", "10".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("DECR".getBytes()),
                new BulkString("counter".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(9, ((RespInteger) result).value());
    }

    @Test
    void decrToZero() {
        store.set("counter", "1".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("DECR".getBytes()),
                new BulkString("counter".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(0, ((RespInteger) result).value());
    }

    @Test
    void decrNonIntegerValueReturnsError() {
        store.set("key", "abc".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("DECR".getBytes()),
                new BulkString("key".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("not an integer"));
    }

    @Test
    void decrWrongArgCount() {
        var result = command.execute(new RespValue[]{
                new BulkString("DECR".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void decrNullKeyReturnsError() {
        var result = command.execute(new RespValue[]{
                new BulkString("DECR".getBytes()),
                BulkString.NULL
        });
        assertInstanceOf(RespError.class, result);
    }
}
