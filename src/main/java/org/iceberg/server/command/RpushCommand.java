package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;

public class RpushCommand implements Command {
    private final Store store;

    public RpushCommand(Store store) {
        this.store = store;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length < 3) {
            return new RespError("ERR wrong number of arguments for 'rpush' command");
        }
        if (!(args[1] instanceof BulkString(byte[] key)) || key == null) {
            return new RespError("ERR key must be a bulk string");
        }
        String keyStr = new String(key);
        byte[][] values = new byte[args.length - 2][];
        for (int i = 2; i < args.length; i++) {
            if (!(args[i] instanceof BulkString(byte[] value))) {
                return new RespError("ERR value must be a bulk string");
            }
            values[i - 2] = value;
        }
        try {
            store.rpush(keyStr, values);
            return new RespInteger(store.llen(keyStr));
        } catch (Store.StoreTypeException e) {
            return new RespError(e.getMessage());
        }
    }
}
