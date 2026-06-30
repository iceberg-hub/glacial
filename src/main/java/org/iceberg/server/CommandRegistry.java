package org.iceberg.server;

import org.iceberg.resp.*;
import org.iceberg.server.command.Command;
import org.iceberg.server.command.EchoCommand;
import org.iceberg.server.command.PingCommand;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandRegistry {

    private final Map<String, Command> commands;
    private static final Logger LOG = Logger.getLogger(CommandRegistry.class.getName());

    public CommandRegistry() {
        this.commands = Map.of(
            "PING", new PingCommand(),
            "ECHO", new EchoCommand()
        );
    }

    public RespValue execute(RespValue request) {
        if (request instanceof Array(RespValue[] items)) {
            if (items == null || items.length == 0) {
                return new RespError("ERR empty command");
            }
            if (!(items[0] instanceof BulkString(byte[] name))) {
                return new RespError("ERR command name must be bulk string");
            }
            if (name == null) {
                return new RespError("ERR empty command name");
            }
            var commandName = new String(name, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
            var command = commands.get(commandName);
            if (command == null) {
                return new RespError("ERR unknown command '" + commandName + "'");
            }
            var response = command.execute(items);
            LOG.log(Level.INFO, "Command execution response: " + response.toString());
            return response;
        }
        return new RespError("ERR expected array command");
    }
}
