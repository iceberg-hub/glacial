package org.iceberg.resp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RespProtocolTest {

    @Test
    void parseSimpleString() {
        var result = RespParser.parse("+OK\r\n".getBytes());
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
    }

    @Test
    void parseSimpleStringWithSpaces() {
        var result = RespParser.parse("+hello world\r\n".getBytes());
        assertInstanceOf(SimpleString.class, result);
        assertEquals("hello world", ((SimpleString) result).value());
    }

    @Test
    void parseError() {
        var result = RespParser.parse("-Error message\r\n".getBytes());
        assertInstanceOf(RespError.class, result);
        assertEquals("Error message", ((RespError) result).value());
    }

    @Test
    void parseInteger() {
        var result = RespParser.parse(":0\r\n".getBytes());
        assertInstanceOf(RespInteger.class, result);
        assertEquals(0, ((RespInteger) result).value());
    }

    @Test
    void parseNegativeInteger() {
        var result = RespParser.parse(":-1\r\n".getBytes());
        assertInstanceOf(RespInteger.class, result);
        assertEquals(-1, ((RespInteger) result).value());
    }

    @Test
    void parseBulkString() {
        var result = RespParser.parse("$5\r\nhello\r\n".getBytes());
        assertInstanceOf(BulkString.class, result);
        assertArrayEquals("hello".getBytes(), ((BulkString) result).value());
    }

    @Test
    void parseEmptyBulkString() {
        var result = RespParser.parse("$0\r\n\r\n".getBytes());
        assertInstanceOf(BulkString.class, result);
        assertEquals(0, ((BulkString) result).value().length);
    }

    @Test
    void parseNullBulkString() {
        var result = RespParser.parse("$-1\r\n".getBytes());
        assertInstanceOf(BulkString.class, result);
        assertNull(((BulkString) result).value());
    }

    @Test
    void parseNullArray() {
        var result = RespParser.parse("*-1\r\n".getBytes());
        assertInstanceOf(Array.class, result);
        assertNull(((Array) result).value());
    }

    @Test
    void parseArrayOfBulkStrings() {
        var result = RespParser.parse("*2\r\n$4\r\necho\r\n$11\r\nhello world\r\n".getBytes());
        assertInstanceOf(Array.class, result);
        var arr = (Array) result;
        assertEquals(2, arr.value().length);
        assertArrayEquals("echo".getBytes(), ((BulkString) arr.value()[0]).value());
        assertArrayEquals("hello world".getBytes(), ((BulkString) arr.value()[1]).value());
    }

    @Test
    void parsePingCommand() {
        var result = RespParser.parse("*1\r\n$4\r\nping\r\n".getBytes());
        assertInstanceOf(Array.class, result);
        var arr = (Array) result;
        assertEquals(1, arr.value().length);
        assertArrayEquals("ping".getBytes(), ((BulkString) arr.value()[0]).value());
    }

    @Test
    void parseGetCommand() {
        var result = RespParser.parse("*2\r\n$3\r\nget\r\n$3\r\nkey\r\n".getBytes());
        assertInstanceOf(Array.class, result);
        var arr = (Array) result;
        assertEquals(2, arr.value().length);
        assertArrayEquals("get".getBytes(), ((BulkString) arr.value()[0]).value());
        assertArrayEquals("key".getBytes(), ((BulkString) arr.value()[1]).value());
    }

    @Test
    void parseEmptyArray() {
        var result = RespParser.parse("*0\r\n".getBytes());
        assertInstanceOf(Array.class, result);
        assertEquals(0, ((Array) result).value().length);
    }

    @Test
    void parseMixedArray() {
        var result = RespParser.parse("*2\r\n+OK\r\n:42\r\n".getBytes());
        assertInstanceOf(Array.class, result);
        var arr = (Array) result;
        assertEquals(2, arr.value().length);
        assertEquals("OK", ((SimpleString) arr.value()[0]).value());
        assertEquals(42, ((RespInteger) arr.value()[1]).value());
    }

    @Test
    void invalidFirstByte() {
        assertThrows(IllegalArgumentException.class,
                () -> RespParser.parse("!invalid\r\n".getBytes()));
    }

    @Test
    void incompleteData() {
        assertThrows(IllegalArgumentException.class,
                () -> RespParser.parse("$5\r\nhel".getBytes()));
    }

    @Test
    void invalidInteger() {
        assertThrows(IllegalArgumentException.class,
                () -> RespParser.parse(":abc\r\n".getBytes()));
    }

    @ParameterizedTest
    @MethodSource("roundTripCases")
    void roundTrip(String wire, RespValue value) {
        var parsed = RespParser.parse(wire.getBytes());
        assertEquals(value, parsed);
        var serialized = new String(RespParser.serialize(parsed));
        assertEquals(wire, serialized);
    }

    static Stream<Arguments> roundTripCases() {
        return Stream.of(
                Arguments.of("+OK\r\n", new SimpleString("OK")),
                Arguments.of("+hello world\r\n", new SimpleString("hello world")),
                Arguments.of("-Error message\r\n", new RespError("Error message")),
                Arguments.of(":0\r\n", new RespInteger(0)),
                Arguments.of(":-1\r\n", new RespInteger(-1)),
                Arguments.of(":42\r\n", new RespInteger(42)),
                Arguments.of("$0\r\n\r\n", new BulkString(new byte[0])),
                Arguments.of("$5\r\nhello\r\n", new BulkString("hello".getBytes())),
                Arguments.of("$-1\r\n", BulkString.NULL),
                Arguments.of("*0\r\n", new Array(new RespValue[0])),
                Arguments.of("*-1\r\n", Array.NULL),
                Arguments.of("*1\r\n$4\r\nping\r\n",
                        new Array(new RespValue[]{new BulkString("ping".getBytes())})),
                Arguments.of("+OK\r\n", new SimpleString("OK"))
        );
    }
}
