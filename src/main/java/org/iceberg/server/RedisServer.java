package org.iceberg.server;

import org.iceberg.resp.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RedisServer {

    private static final Logger LOG = Logger.getLogger(RedisServer.class.getName());

    private final int port;
    private final CommandRegistry commandRegistry;

    public RedisServer(int port) {
        this(port, Path.of("dump.rdb"));
    }

    public RedisServer(int port, Path savePath) {
        this.port = port;
        var store = new Store();
        Persistence.load(store, savePath);
        this.commandRegistry = new CommandRegistry(store, savePath);
    }

    public void start() {
        try (var server = new ServerSocket(port)) {
            System.out.println("Glacial server listening on port " + port);
            while (true) {
                try {
                    var socket = server.accept();
                    socket.setTcpNoDelay(true);
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
            var in = new PushbackInputStream(new BufferedInputStream(socket.getInputStream(), 8192), 1);
            var out = new BufferedOutputStream(socket.getOutputStream(), 8192);
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
                if (RespParser.isOk(response)) {
                    RespParser.serializeOk(out);
                } else {
                    RespParser.serialize(response, out);
                }
                out.flush();
            }
        } catch (UncheckedIOException e) {
            LOG.log(Level.FINE, "Client disconnected", e.getCause());
        } catch (IOException e) {
            LOG.log(Level.FINE, "Client disconnected", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error handling client", e);
        }
    }
}
