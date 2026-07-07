package org.iceberg.server.async;

import org.iceberg.resp.*;
import org.iceberg.server.CommandRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Connection {

    private static final Logger LOG = Logger.getLogger(Connection.class.getName());

    private final AsynchronousSocketChannel channel;
    private final CommandRegistry commandRegistry;
    private final RespDecoder decoder;
    private final ByteBuffer readBuf = ByteBuffer.allocate(8192);
    private final List<byte[]> pendingWrites = new ArrayList<>();
    private boolean writing = false;

    public Connection(AsynchronousSocketChannel channel, CommandRegistry commandRegistry) {
        this.channel = channel;
        this.commandRegistry = commandRegistry;
        this.decoder = new RespDecoder();
    }

    public void read() {
        readBuf.clear();
        channel.read(readBuf, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer bytesRead, Void attachment) {
                if (bytesRead == -1) {
                    close();
                    return;
                }
                readBuf.flip();
                var data = new byte[readBuf.remaining()];
                readBuf.get(data);
                processData(data);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                close();
            }
        });
    }

    private void processData(byte[] data) {
        var offset = 0;
        while (offset < data.length) {
            var result = decoder.tryParseOne(data, offset);
            if (result == null) {
                break;
            }
            var request = result.value();
            offset += result.consumed();

            try {
                var response = commandRegistry.execute(request);
                var serialized = RespParser.serialize(response);
                write(serialized);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Command execution error", e);
                write(RespParser.serialize(new RespError("ERR " + e.getMessage())));
            }
        }
        if (offset == 0 && data.length > 0) {
            LOG.warning("Incomplete RESP message, discarding");
        }
        read();
    }

    public synchronized void write(byte[] data) {
        pendingWrites.add(data);
        if (!writing) {
            writing = true;
            doWrite();
        }
    }

    private synchronized void doWrite() {
        if (pendingWrites.isEmpty()) {
            writing = false;
            return;
        }
        var data = pendingWrites.remove(0);
        var buf = ByteBuffer.wrap(data);
        channel.write(buf, buf, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer buffer) {
                if (buffer.hasRemaining()) {
                    channel.write(buffer, buffer, this);
                } else {
                    doWrite();
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer buffer) {
                LOG.log(Level.WARNING, "Write failed", exc);
                close();
            }
        });
    }

    private void close() {
        try {
            channel.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error closing channel", e);
        }
    }
}
