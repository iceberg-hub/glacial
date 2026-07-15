package org.iceberg.server.command;

import org.iceberg.resp.Array;
import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;

public class LrangeCommand implements Command {
    private final Store store;

    public LrangeCommand(Store store) {
        this.store = store;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length != 4) {
            return new RespError("ERR wrong number of arguments for 'lrange' command");
        }
        if (!(args[1] instanceof BulkString(byte[] key)) || key == null) {
            return new RespError("ERR key must be a bulk string");
        }
        long start;
        long stop;
        try {
            start = Long.parseLong(new String(args[2] instanceof BulkString(byte[] v) ? v : new byte[0]));
            stop = Long.parseLong(new String(args[3] instanceof BulkString(byte[] v) ? v : new byte[0]));
        } catch (NumberFormatException e) {
            return new RespError("ERR value is not an integer or out of range");
        }
        var elements = store.lrange(new String(key), start, stop);
        var result = new RespValue[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            result[i] = new BulkString(elements.get(i));
        }
        return new Array(result);
    }
}
