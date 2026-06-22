package org.iceberg.server;

import org.iceberg.resp.*;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RedisServer {

    private static final Logger LOG = Logger.getLogger(RedisServer.class.getName());

    private final int port;
    private final CommandRegistry commandRegistry;

    public RedisServer(int port) {
        this.port = port;
        this.commandRegistry = new CommandRegistry();
    }

    public void start() {
        try (var server = new ServerSocket(port)) {
            System.out.println("Redis Lite server listening on port " + port);
            while (true) {
                try {
                    var socket = server.accept();
                    Thread.ofVirtual()
                            .name("redis-client")
                            .start(() -> handleClient(socket));
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Server socket error", e);
                    break;
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Server failed", e);
        }
    }

    private void handleClient(Socket socket) {
        try (socket) {
            var in = new PushbackInputStream(socket.getInputStream(), 1);
            var out = socket.getOutputStream();
            while (true) {
                int first = in.read();
                if (first == -1) {
                    return;
                }
                in.unread(first);
                RespValue request;
                try {
                    request = RespParser.parse(in);
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "Malformed request from client", e);
                    try {
                        RespParser.serialize(new RespError("ERR Protocol error"), out);
                        out.flush();
                    } catch (IOException ignored) {
                    }
                    return;
                }
                var response = commandRegistry.execute(request);
                RespParser.serialize(response, out);
                out.flush();
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Client disconnected", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error handling client", e);
        }
    }
}
