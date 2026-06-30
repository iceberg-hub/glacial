package org.iceberg.server.command;

import org.iceberg.resp.RespValue;
import org.iceberg.resp.SimpleString;

public class PingCommand implements Command {
    @Override
    public RespValue execute(RespValue[] args) {
        return new SimpleString("PONG");
    }
}
