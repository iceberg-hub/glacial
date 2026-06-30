package org.iceberg.server.command;

import org.iceberg.resp.SimpleString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingCommandTest {

    private final PingCommand command = new PingCommand();

    @Test
    void returnsPong() {
        var result = command.execute(new org.iceberg.resp.RespValue[0]);
        assertInstanceOf(SimpleString.class, result);
        assertEquals("PONG", ((SimpleString) result).value());
    }
}
