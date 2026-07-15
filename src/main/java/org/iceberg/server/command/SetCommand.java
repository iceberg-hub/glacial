package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;
import org.iceberg.resp.SimpleString;
import org.iceberg.server.Store;

import java.nio.charset.StandardCharsets;

public class SetCommand implements Command {
    private final Store store;

    public SetCommand(Store store) {
        this.store = store;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length < 3) {
            return new RespError("ERR wrong number of arguments for 'set' command");
        }
        if (!(args[1] instanceof BulkString(byte[] value1)) || value1 == null) {
            return new RespError("ERR key must be a bulk string");
        }
        if (!(args[2] instanceof BulkString(byte[] value))) {
            return new RespError("ERR value must be a bulk string");
        }

        long expiresAtMillis = Store.NO_EXPIRY;
        int i = 3;

        while (i < args.length) {
            if (!(args[i] instanceof BulkString(byte[] tokenBytes))) {
                return new RespError("ERR syntax error");
            }
            String token = new String(tokenBytes, StandardCharsets.UTF_8).toUpperCase();

            switch (token) {
                case "EX", "PX" -> {
                    i++;
                    if (i >= args.length) {
                        return new RespError("ERR syntax error");
                    }
                    if (!(args[i] instanceof BulkString(byte[] valBytes)) || valBytes == null) {
                        return new RespError("ERR syntax error");
                    }
                    long num;
                    try {
                        num = Long.parseLong(new String(valBytes, StandardCharsets.UTF_8));
                    } catch (NumberFormatException e) {
                        return new RespError("ERR invalid expire time in 'set' command");
                    }
                    if (num <= 0) {
                        return new RespError("ERR invalid expire time in 'set' command");
                    }
                    if (expiresAtMillis != Store.NO_EXPIRY) {
                        return new RespError("ERR syntax error");
                    }
                    expiresAtMillis = token.equals("EX")
                            ? System.currentTimeMillis() + num * 1000
                            : System.currentTimeMillis() + num;
                }
                case "EXAT", "PXAT" -> {
                    i++;
                    if (i >= args.length) {
                        return new RespError("ERR syntax error");
                    }
                    if (!(args[i] instanceof BulkString(byte[] valBytes)) || valBytes == null) {
                        return new RespError("ERR syntax error");
                    }
                    long num;
                    try {
                        num = Long.parseLong(new String(valBytes, StandardCharsets.UTF_8));
                    } catch (NumberFormatException e) {
                        return new RespError("ERR invalid expire time in 'set' command");
                    }
                    if (num <= 0) {
                        return new RespError("ERR invalid expire time in 'set' command");
                    }
                    if (expiresAtMillis != Store.NO_EXPIRY) {
                        return new RespError("ERR syntax error");
                    }
                    expiresAtMillis = token.equals("EXAT") ? num * 1000 : num;
                }
                default -> {
                    return new RespError("ERR syntax error");
                }
            }
            i++;
        }

        store.set(new String(value1, StandardCharsets.UTF_8), value, expiresAtMillis);
        return new SimpleString("OK");
    }
}
