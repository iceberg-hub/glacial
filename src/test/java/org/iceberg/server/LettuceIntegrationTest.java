package org.iceberg.server;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.Duration;
import java.nio.file.Path;

import static org.iceberg.server.RedisTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class LettuceIntegrationTest {

    @TempDir
    Path tempDir;

    private RedisServer server;
    private Thread serverThread;
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> commands;

    @BeforeEach
    void setUp() throws Exception {
        int port = findAvailablePort();
        var savePath = tempDir.resolve("dump.rdb");
        server = new RedisServer(port, savePath);
        serverThread = new Thread(() -> server.start());
        serverThread.setDaemon(true);
        serverThread.start();
        waitForPort(port, Duration.ofSeconds(5));

        client = RedisClient.create("redis://localhost:" + port);
        connection = client.connect();
        commands = connection.sync();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
        serverThread.interrupt();
    }

    @Test
    void setReturnsOk() {
        var result = commands.set("Name", "John");
        assertEquals("OK", result);
    }

    @Test
    void getReturnsValue() {
        commands.set("Name", "John");
        var value = commands.get("Name");
        assertEquals("John", value);
    }

    @Test
    void getNonExistentReturnsNull() {
        var value = commands.get("NonExistent");
        assertNull(value);
    }

    @Test
    void setOverwritesExistingKey() {
        commands.set("key", "value1");
        commands.set("key", "value2");
        var value = commands.get("key");
        assertEquals("value2", value);
    }

    @Test
    void setAndGetMultipleKeys() {
        commands.set("a", "1");
        commands.set("b", "2");
        commands.set("c", "3");
        assertEquals("1", commands.get("a"));
        assertEquals("2", commands.get("b"));
        assertEquals("3", commands.get("c"));
    }

    @Test
    void setEmptyValue() {
        commands.set("empty", "");
        var value = commands.get("empty");
        assertEquals("", value);
    }

    @Test
    void setTooFewArgsReturnsError() {
        var e = assertThrows(RedisCommandExecutionException.class, () ->
            commands.dispatch(
                CommandType.SET,
                new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("orphan")
            )
        );
        assertTrue(e.getMessage().toLowerCase().contains("wrong number of arguments"));
    }

    @Test
    void getTooManyArgsReturnsError() {
        commands.set("k", "v");
        var e = assertThrows(RedisCommandExecutionException.class, () ->
            commands.dispatch(
                CommandType.GET,
                new ValueOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("k").add("junk")
            )
        );
        assertTrue(e.getMessage().toLowerCase().contains("wrong number of arguments"));
    }

    @Test
    void unknownCommandReturnsError() {
        var e = assertThrows(RedisCommandExecutionException.class, () ->
            commands.dispatch(
                CommandType.INFO,
                new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8)
            )
        );
        assertTrue(e.getMessage().toLowerCase().contains("unknown command"));
    }

    @Test
    void getWithNonStringValueType() {
        commands.set("num", "123");
        var result = commands.get("num");
        assertEquals("123", result);
    }

    @Test
    void existsReturnsOneForExistingKey() {
        commands.set("mykey", "myval");
        var result = commands.exists("mykey");
        assertEquals(1L, result);
    }

    @Test
    void existsReturnsZeroForNonExistentKey() {
        var result = commands.exists("nokey");
        assertEquals(0L, result);
    }

    @Test
    void delRemovesKeyAndReturnsOne() {
        commands.set("todel", "val");
        var result = commands.del("todel");
        assertEquals(1L, result);
        assertNull(commands.get("todel"));
    }

    @Test
    void delReturnsZeroForMissingKey() {
        var result = commands.del("ghost");
        assertEquals(0L, result);
    }

    @Test
    void incrNonExistentKeyReturnsOne() {
        var result = commands.incr("mycounter");
        assertEquals(1L, result);
    }

    @Test
    void incrExistingValue() {
        commands.set("cnt", "10");
        var result = commands.incr("cnt");
        assertEquals(11L, result);
    }

    @Test
    void incrNonIntegerReturnsError() {
        commands.set("str", "abc");
        assertThrows(Exception.class, () -> commands.incr("str"));
    }

    @Test
    void decrNonExistentKeyReturnsMinusOne() {
        var result = commands.decr("mycounter");
        assertEquals(-1L, result);
    }

    @Test
    void decrExistingValue() {
        commands.set("cnt", "10");
        var result = commands.decr("cnt");
        assertEquals(9L, result);
    }

    @Test
    void lpushCreatesListAndReturnsLength() {
        var result = commands.lpush("mylist", "a", "b", "c");
        assertEquals(3L, result);
    }

    @Test
    void rpushCreatesListAndReturnsLength() {
        var result = commands.rpush("mylist", "x", "y");
        assertEquals(2L, result);
    }

    @Test
    void lpushThenLrange() {
        commands.lpush("lst", "a", "b", "c");
        var list = commands.lrange("lst", 0, -1);
        assertEquals(3, list.size());
        assertEquals("c", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    void rpushThenLrange() {
        commands.rpush("lst", "a", "b", "c");
        var list = commands.lrange("lst", 0, -1);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    void saveReturnsOk() {
        commands.set("key", "value");
        var result = commands.dispatch(
                CommandType.SAVE,
                new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8)
        );
        assertEquals("OK", result);
    }
}
