package org.iceberg.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

import static org.iceberg.server.RedisTestFixture.*;
import static org.junit.jupiter.api.Assertions.*;

class RedisServerTest {

    private RedisServer server;
    private Thread serverThread;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        port = findAvailablePort();
        server = new RedisServer(port);
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
    void malformedRequestGetsProtocolError() throws Exception {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 5000);
            sendAndFlush(socket.getOutputStream(), "!invalid\r\n");
            assertTrue(readResp(socket.getInputStream()).contains("ERR Protocol error"));
        }
    }
}
