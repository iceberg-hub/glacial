package org.iceberg.server.async;

import org.iceberg.server.CommandRegistry;
import org.iceberg.server.Store;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;

public class AsyncRedisServer {

    private final int port;
    private final CommandRegistry commandRegistry;

    public AsyncRedisServer(int port) {
        this.port = port;
        this.commandRegistry = new CommandRegistry(new Store());
    }

    public void start() throws IOException {
        var serverChannel = AsynchronousServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new java.net.InetSocketAddress(port));
        System.out.println("Async Redis Lite server listening on port " + port);

        var state = new AcceptHandler.State(serverChannel, commandRegistry);
        serverChannel.accept(state, new AcceptHandler());

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
