package org.iceberg.server.command;

import org.iceberg.resp.RespValue;

public interface Command {
    RespValue execute(RespValue[] args);
}
