package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;

public class DelCommand implements Command {
    private final Store store;

    public DelCommand(Store store) {
        this.store = store;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length < 2) {
            return new RespError("ERR wrong number of arguments for 'del' command");
        }
        String[] keys = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            if (!(args[i] instanceof BulkString(byte[] key)) || key == null) {
                return new RespError("ERR key must be a bulk string");
            }
            keys[i - 1] = new String(key);
        }
        return new RespInteger(store.delete(keys));
    }
}
