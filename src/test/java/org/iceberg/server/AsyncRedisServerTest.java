package org.iceberg.server;

import org.iceberg.server.async.AsyncRedisServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

import static org.iceberg.server.RedisTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncRedisServerTest {

    private AsyncRedisServer server;
    private Thread serverThread;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        port = findAvailablePort();
        server = new AsyncRedisServer(port);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
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
    void getNonExistent() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*2\r\n$3\r\nGET\r\n$11\r\nNonExistent\r\n");
            assertEquals("$-1", readResp(socket.getInputStream()));
        }
    }

    @Test
    void echo() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n");
            assertEquals("$5", readResp(socket.getInputStream()));
            assertEquals("hello", readBytes(socket.getInputStream(), 5));
        }
    }
}
