package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncrCommandTest {

    private final Store store = new Store();
    private final IncrCommand command = new IncrCommand(store);

    @Test
    void incrNonExistentKeyReturnsOne() {
        var result = command.execute(new RespValue[]{
                new BulkString("INCR".getBytes()),
                new BulkString("counter".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(1, ((RespInteger) result).value());
    }

    @Test
    void incrExistingInteger() {
        store.set("counter", "10".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("INCR".getBytes()),
                new BulkString("counter".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(11, ((RespInteger) result).value());
    }

    @Test
    void incrNegativeValue() {
        store.set("counter", "-5".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("INCR".getBytes()),
                new BulkString("counter".getBytes())
        });
        assertInstanceOf(RespInteger.class, result);
        assertEquals(-4, ((RespInteger) result).value());
    }

    @Test
    void incrNonIntegerValueReturnsError() {
        store.set("key", "abc".getBytes());
        var result = command.execute(new RespValue[]{
                new BulkString("INCR".getBytes()),
                new BulkString("key".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("not an integer"));
    }

    @Test
    void incrWrongArgCount() {
        var result = command.execute(new RespValue[]{
                new BulkString("INCR".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void incrNullKeyReturnsError() {
        var result = command.execute(new RespValue[]{
                new BulkString("INCR".getBytes()),
                BulkString.NULL
        });
        assertInstanceOf(RespError.class, result);
    }
}
