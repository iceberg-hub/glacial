package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.RespInteger;
import org.iceberg.resp.RespValue;
import org.iceberg.server.Store;

public class DecrCommand implements Command {
    private final Store store;

    public DecrCommand(Store store) {
        this.store = store;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length != 2) {
            return new RespError("ERR wrong number of arguments for 'decr' command");
        }
        if (!(args[1] instanceof BulkString(byte[] key)) || key == null) {
            return new RespError("ERR key must be a bulk string");
        }
        try {
            long result = store.decrement(new String(key));
            return new RespInteger(result);
        } catch (NumberFormatException e) {
            return new RespError("ERR value is not an integer or out of range");
        } catch (Store.StoreTypeException e) {
            return new RespError(e.getMessage());
        }
    }
}
