package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;

public class GetCommand implements Command {
    private final Store store;

    public GetCommand(Store store) {
        this.store = store;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length != 2) {
            return new RespError("ERR wrong number of arguments for 'get' command");
        }
        if (!(args[1] instanceof BulkString(byte[] value1)) || value1 == null) {
            return new RespError("ERR key must be a bulk string");
        }
        var value = store.get(new String(value1));
        if (value == null) {
            return BulkString.NULL;
        }
        return new BulkString(value);
    }
}
