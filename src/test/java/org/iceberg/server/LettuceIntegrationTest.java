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

import java.time.Duration;

import static org.iceberg.server.RedisTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class LettuceIntegrationTest {

    private RedisServer server;
    private Thread serverThread;
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> commands;

    @BeforeEach
    void setUp() throws Exception {
        int port = findAvailablePort();
        server = new RedisServer(port);
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
}
