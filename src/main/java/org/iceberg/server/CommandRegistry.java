package org.iceberg.server;

import org.iceberg.resp.*;
import org.iceberg.server.command.Command;
import org.iceberg.server.command.DecrCommand;
import org.iceberg.server.command.DelCommand;
import org.iceberg.server.command.EchoCommand;
import org.iceberg.server.command.ExistsCommand;
import org.iceberg.server.command.GetCommand;
import org.iceberg.server.command.IncrCommand;
import org.iceberg.server.command.LpushCommand;
import org.iceberg.server.command.LrangeCommand;
import org.iceberg.server.command.PingCommand;
import org.iceberg.server.command.RpushCommand;
import org.iceberg.server.command.SetCommand;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandRegistry {

    private final Map<String, Command> commands;
    private static final Logger LOG = Logger.getLogger(CommandRegistry.class.getName());

    public CommandRegistry(Store store) {
        this.commands = new HashMap<>(Map.of(
            "PING", new PingCommand(),
            "ECHO", new EchoCommand(),
            "SET", new SetCommand(store),
            "GET", new GetCommand(store),
            "EXISTS", new ExistsCommand(store),
            "DEL", new DelCommand(store),
            "INCR", new IncrCommand(store),
            "DECR", new DecrCommand(store),
            "LPUSH", new LpushCommand(store),
            "RPUSH", new RpushCommand(store)
        ));
        this.commands.put("LRANGE", new LrangeCommand(store));
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
            LOG.log(Level.FINE, "Command execution response: {0}", response);
            return response;
        }
        return new RespError("ERR expected array command");
    }
}
