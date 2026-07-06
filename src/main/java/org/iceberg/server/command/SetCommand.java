package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;
import org.iceberg.resp.SimpleString;
import org.iceberg.server.Store;

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
        store.set(new String(value1), value);
        return new SimpleString("OK");
    }
}
