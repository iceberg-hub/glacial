package org.iceberg.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;

import static org.iceberg.server.RedisTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class RedisServerTest {

    @TempDir
    Path tempDir;

    private RedisServer server;
    private Thread serverThread;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        port = findAvailablePort();
        var savePath = tempDir.resolve("dump.rdb");
        server = new RedisServer(port, savePath);
        serverThread = new Thread(() -> server.start());
        serverThread.setDaemon(true);
        serverThread.start();
        waitForPort(port, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        serverThread.interrupt();
    }

    @Test
    void ping() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*1\r\n$4\r\nPING\r\n");
            assertEquals("+PONG", readResp(socket.getInputStream()));
        }
    }

    @Test
    void echo() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*2\r\n$4\r\nECHO\r\n$11\r\nHello World\r\n");
            assertEquals("$11", readResp(in));
            assertEquals("Hello World", readBytes(in, 11));
        }
    }

    @Test
    void setAndGet() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$4\r\nName\r\n$4\r\nJohn\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*2\r\n$3\r\nGET\r\n$4\r\nName\r\n");
            assertEquals("$4", readResp(in));
            assertEquals("John", readBytes(in, 4));
        }
    }

    @Test
    void getNonExistentKeyReturnsNull() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*2\r\n$3\r\nGET\r\n$11\r\nNonExistent\r\n");
            assertEquals("$-1", readResp(socket.getInputStream()));
        }
    }

    @Test
    void setOverwritesExistingKey() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$6\r\nvalue1\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$6\r\nvalue2\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
            assertEquals("$6", readResp(in));
            assertEquals("value2", readBytes(in, 6));
        }
    }

    @Test
    void malformedRequestGetsProtocolError() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "!invalid\r\n");
            assertTrue(readResp(socket.getInputStream()).contains("ERR Protocol error"));
        }
    }

    @Test
    void existsReturnsOneForExistingKey() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*2\r\n$6\r\nEXISTS\r\n$3\r\nfoo\r\n");
            assertEquals(":1", readResp(in));
        }
    }

    @Test
    void existsReturnsZeroForMissingKey() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*2\r\n$6\r\nEXISTS\r\n$7\r\nmissing\r\n");
            assertEquals(":0", readResp(socket.getInputStream()));
        }
    }

    @Test
    void delRemovesKey() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*2\r\n$3\r\nDEL\r\n$3\r\nkey\r\n");
            assertEquals(":1", readResp(in));

            sendAndFlush(out, "*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
            assertEquals("$-1", readResp(in));
        }
    }

    @Test
    void incrNonExistentKeyReturnsOne() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*2\r\n$4\r\nINCR\r\n$7\r\ncounter\r\n");
            assertEquals(":1", readResp(socket.getInputStream()));
        }
    }

    @Test
    void incrExistingValue() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$7\r\ncounter\r\n$2\r\n10\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*2\r\n$4\r\nINCR\r\n$7\r\ncounter\r\n");
            assertEquals(":11", readResp(in));
        }
    }

    @Test
    void incrNonIntegerReturnsError() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$3\r\nabc\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*2\r\n$4\r\nINCR\r\n$3\r\nkey\r\n");
            assertTrue(readResp(in).contains("not an integer"));
        }
    }

    @Test
    void decrNonExistentKeyReturnsMinusOne() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*2\r\n$4\r\nDECR\r\n$7\r\ncounter\r\n");
            assertEquals(":-1", readResp(socket.getInputStream()));
        }
    }

    @Test
    void lpushCreatesListAndReturnsLength() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*4\r\n$5\r\nLPUSH\r\n$6\r\nmylist\r\n$1\r\na\r\n$1\r\nb\r\n");
            assertEquals(":2", readResp(in));
        }
    }

    @Test
    void rpushCreatesListAndReturnsLength() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*4\r\n$5\r\nRPUSH\r\n$6\r\nmylist\r\n$1\r\na\r\n$1\r\nb\r\n");
            assertEquals(":2", readResp(in));
        }
    }

    @Test
    void lpushThenGetReturnsNull() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*3\r\n$5\r\nLPUSH\r\n$3\r\nlst\r\n$3\r\nfoo\r\n");
            assertEquals(":1", readResp(in));
            sendAndFlush(out, "*2\r\n$3\r\nGET\r\n$3\r\nlst\r\n");
            assertEquals("$-1", readResp(in));
        }
    }

    @Test
    void saveThenLoadPersistsData() throws Exception {
        var savePath = tempDir.resolve("dump.rdb");
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();

            sendAndFlush(out, "*3\r\n$3\r\nSET\r\n$5\r\nmykey\r\n$7\r\nmyvalue\r\n");
            assertEquals("+OK", readResp(in));

            sendAndFlush(out, "*1\r\n$4\r\nSAVE\r\n");
            assertEquals("+OK", readResp(in));
        }

        serverThread.interrupt();
        serverThread.join(2000);

        port = findAvailablePort();
        var server2 = new RedisServer(port, savePath);
        var thread2 = new Thread(() -> server2.start());
        thread2.setDaemon(true);
        thread2.start();
        waitForPort(port, Duration.ofSeconds(5));

        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            sendAndFlush(out, "*2\r\n$3\r\nGET\r\n$5\r\nmykey\r\n");
            assertEquals("$7", readResp(in));
            assertEquals("myvalue", readBytes(in, 7));
        }

        thread2.interrupt();
    }
}
