package org.iceberg.server.command;

import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;

public class EchoCommand implements Command {
    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length < 2) {
            return new RespError("ERR wrong number of arguments for 'echo' command");
        }
        return args[1];
    }
}
