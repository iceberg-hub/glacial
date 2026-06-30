package org.iceberg.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

public class RedisTestFixture {

    public static void sendAndFlush(OutputStream out, String data) throws IOException {
        out.write(data.getBytes());
        out.flush();
    }

    public static String readResp(InputStream in) throws IOException {
        var buf = new StringBuilder();
        int prev = in.read();
        if (prev == -1) throw new IOException("EOF");
        while (true) {
            int cur = in.read();
            if (cur == -1) throw new IOException("EOF");
            if (prev == '\r' && cur == '\n') break;
            buf.append((char) prev);
            prev = cur;
        }
        return buf.toString();
    }

    public static String readBytes(InputStream in, int count) throws IOException {
        var buf = new byte[count];
        int offset = 0;
        while (offset < count) {
            int read = in.read(buf, offset, count - offset);
            if (read == -1) throw new IOException("EOF");
            offset += read;
        }
        return new String(buf);
    }

    public static int findAvailablePort() throws IOException {
        try (var s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    public static void waitForPort(int port, Duration timeout) throws InterruptedException {
        var deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try (var s = new Socket()) {
                s.connect(new InetSocketAddress("localhost", port), 200);
                return;
            } catch (IOException ignored) {
                Thread.sleep(50);
            }
        }
        throw new RuntimeException("Server did not start on port " + port);
    }
}
