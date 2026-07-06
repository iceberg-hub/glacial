package org.iceberg.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RespClient implements AutoCloseable {

    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;

    public RespClient(String host, int port, int timeout) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        socket.setTcpNoDelay(true);
        out = socket.getOutputStream();
        in = socket.getInputStream();
    }

    public void sendCommand(String... args) throws IOException {
        var cmd = new StringBuilder();
        cmd.append("*").append(args.length).append("\r\n");
        for (var arg : args) {
            cmd.append("$").append(arg.length()).append("\r\n").append(arg).append("\r\n");
        }
        out.write(cmd.toString().getBytes());
        out.flush();
    }

    public String readStatus() throws IOException {
        var line = readLine();
        if (line.startsWith("+")) {
            return line.substring(1);
        }
        throw new IOException("Unexpected status response: " + line);
    }

    public String readBulkString() throws IOException {
        var line = readLine();
        if ("$-1".equals(line)) {
            return null;
        }
        if (line.startsWith("$")) {
            var len = Integer.parseInt(line.substring(1));
            var data = readBytes(len);
            readBytes(2);
            return data;
        }
        throw new IOException("Unexpected bulk string response: " + line);
    }

    private String readLine() throws IOException {
        var buf = new StringBuilder();
        int prev = in.read();
        if (prev == -1) throw new IOException("Connection closed");
        while (true) {
            int cur = in.read();
            if (cur == -1) throw new IOException("Connection closed");
            if (prev == '\r' && cur == '\n') break;
            buf.append((char) prev);
            prev = cur;
        }
        return buf.toString();
    }

    private String readBytes(int count) throws IOException {
        var buf = new byte[count];
        int offset = 0;
        while (offset < count) {
            int read = in.read(buf, offset, count - offset);
            if (read == -1) throw new IOException("Connection closed");
            offset += read;
        }
        return new String(buf);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
