package org.iceberg.server;

import org.iceberg.resp.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private final CommandRegistry registry = new CommandRegistry(new Store());

    @Test
    void ping() {
        var request = new Array(new RespValue[]{
                new BulkString("PING".getBytes())
        });
        var result = registry.execute(request);
        assertInstanceOf(SimpleString.class, result);
        assertEquals("PONG", ((SimpleString) result).value());
    }

    @Test
    void pingLowercase() {
        var request = new Array(new RespValue[]{
                new BulkString("ping".getBytes())
        });
        var result = registry.execute(request);
        assertInstanceOf(SimpleString.class, result);
        assertEquals("PONG", ((SimpleString) result).value());
    }

    @Test
    void echo() {
        var request = new Array(new RespValue[]{
                new BulkString("ECHO".getBytes()),
                new BulkString("hello".getBytes())
        });
        var result = registry.execute(request);
        assertInstanceOf(BulkString.class, result);
        assertArrayEquals("hello".getBytes(), ((BulkString) result).value());
    }

    @Test
    void echoWithNoArgReturnsError() {
        var request = new Array(new RespValue[]{
                new BulkString("ECHO".getBytes())
        });
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
    }

    @Test
    void unknownCommandReturnsError() {
        var request = new Array(new RespValue[]{
                new BulkString("UNKNOWN".getBytes())
        });
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("unknown command"));
    }

    @Test
    void nullArrayReturnsError() {
        var request = Array.NULL;
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
    }

    @Test
    void emptyArrayReturnsError() {
        var request = new Array(new RespValue[0]);
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
    }

    @Test
    void firstElementNotBulkStringReturnsError() {
        var request = new Array(new RespValue[]{
                new RespInteger(123)
        });
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("must be bulk string"));
    }

    @Test
    void nullBulkStringNameReturnsError() {
        var request = new Array(new RespValue[]{
                BulkString.NULL
        });
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("empty command name"));
    }

    @Test
    void nonArrayRequestReturnsError() {
        var request = new SimpleString("PING");
        var result = registry.execute(request);
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("expected array"));
    }
}
