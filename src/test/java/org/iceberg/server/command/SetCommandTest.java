package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;
import org.iceberg.resp.SimpleString;
import org.iceberg.server.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetCommandTest {

    private final Store store = new Store();
    private final SetCommand command = new SetCommand(store);

    private RespValue[] args(String... values) {
        var result = new RespValue[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = new BulkString(values[i].getBytes());
        }
        return result;
    }

    @Test
    void setsValue() {
        var result = command.execute(args("SET", "Name", "John"));
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
    }

    @Test
    void errorWhenNoArguments() {
        var result = command.execute(args("SET"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenMissingValue() {
        var result = command.execute(args("SET", "key"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }

    @Test
    void errorWhenKeyIsNull() {
        var args = new RespValue[]{
                new BulkString("SET".getBytes()),
                BulkString.NULL,
                new BulkString("value".getBytes())
        };
        var result = command.execute(args);
        assertInstanceOf(RespError.class, result);
    }

    @Test
    void setsValueWithExpiry() {
        var result = command.execute(args("SET", "key", "value", "EX", "10"));
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
        assertNotNull(store.get("key"));
    }

    @Test
    void setsValueWithPx() {
        var result = command.execute(args("SET", "key", "value", "PX", "5000"));
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
        assertNotNull(store.get("key"));
    }

    @Test
    void setsValueWithExat() {
        long futureSeconds = (System.currentTimeMillis() / 1000) + 60;
        var result = command.execute(args("SET", "key", "value", "EXAT", String.valueOf(futureSeconds)));
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
        assertNotNull(store.get("key"));
    }

    @Test
    void setsValueWithPxat() {
        long futureMillis = System.currentTimeMillis() + 60000;
        var result = command.execute(args("SET", "key", "value", "PXAT", String.valueOf(futureMillis)));
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
        assertNotNull(store.get("key"));
    }

    @Test
    void errorOnNegativeExpiry() {
        var result = command.execute(args("SET", "key", "value", "EX", "-1"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("invalid expire time"));
    }

    @Test
    void errorOnZeroExpiry() {
        var result = command.execute(args("SET", "key", "value", "EX", "0"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("invalid expire time"));
    }

    @Test
    void errorOnNonNumericExpiry() {
        var result = command.execute(args("SET", "key", "value", "EX", "abc"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("invalid expire time"));
    }

    @Test
    void errorOnMissingExpiryValue() {
        var result = command.execute(args("SET", "key", "value", "EX"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("syntax error"));
    }

    @Test
    void errorOnDuplicateExpiryOptions() {
        var result = command.execute(args("SET", "key", "value", "EX", "10", "PX", "5000"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("syntax error"));
    }

    @Test
    void errorOnUnknownOption() {
        var result = command.execute(args("SET", "key", "value", "NX"));
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("syntax error"));
    }

    @Test
    void keyExpiresAfterExDuration() {
        command.execute(args("SET", "key", "value", "PX", "100"));
        assertNotNull(store.get("key"));
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertNull(store.get("key"));
    }

    @Test
    void keyExpiresAfterExatTime() {
        long expireAt = System.currentTimeMillis() + 100;
        var result = command.execute(args("SET", "key", "value", "PXAT", String.valueOf(expireAt)));
        assertInstanceOf(SimpleString.class, result);
        assertNotNull(store.get("key"));
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertNull(store.get("key"));
    }
}
