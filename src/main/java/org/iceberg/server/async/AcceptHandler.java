package org.iceberg.server.async;

import org.iceberg.server.CommandRegistry;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AcceptHandler.State> {

    private static final Logger LOG = Logger.getLogger(AcceptHandler.class.getName());

    public record State(AsynchronousServerSocketChannel serverChannel, CommandRegistry commandRegistry) {}

    @Override
    public void completed(AsynchronousSocketChannel client, State state) {
        state.serverChannel().accept(state, this);
        try {
            client.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not set TCP_NODELAY", e);
        }
        var connection = new Connection(client, state.commandRegistry());
        connection.read();
    }

    @Override
    public void failed(Throwable exc, State state) {
        LOG.log(Level.SEVERE, "Accept failed", exc);
    }
}
