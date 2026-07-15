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
import org.iceberg.server.command.SaveCommand;
import org.iceberg.server.command.SetCommand;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandRegistry {

    private final Map<String, Command> commands;
    private static final Logger LOG = Logger.getLogger(CommandRegistry.class.getName());

    public CommandRegistry(Store store, Path savePath) {
        this.commands = Map.ofEntries(
            Map.entry("PING", new PingCommand()),
            Map.entry("ECHO", new EchoCommand()),
            Map.entry("SET", new SetCommand(store)),
            Map.entry("GET", new GetCommand(store)),
            Map.entry("EXISTS", new ExistsCommand(store)),
            Map.entry("DEL", new DelCommand(store)),
            Map.entry("INCR", new IncrCommand(store)),
            Map.entry("DECR", new DecrCommand(store)),
            Map.entry("LPUSH", new LpushCommand(store)),
            Map.entry("RPUSH", new RpushCommand(store)),
            Map.entry("LRANGE", new LrangeCommand(store)),
            Map.entry("SAVE", new SaveCommand(store, savePath))
        );
    }

    public RespValue execute(RespValue request) {
        if (!(request instanceof Array(RespValue[] items))) {
            return new RespError("ERR expected array command");
        }
        if (items == null || items.length == 0) {
            return new RespError("ERR empty command");
        }
        if (!(items[0] instanceof BulkString(byte[] name))) {
            return new RespError("ERR command name must be bulk string");
        }
        if (name == null) {
            return new RespError("ERR empty command name");
        }
        var commandName = toUpperCase(name);
        var command = commands.get(commandName);
        if (command == null) {
            return new RespError("ERR unknown command '" + commandName + "'");
        }
        var response = command.execute(items);
        LOG.log(Level.FINE, "Command execution response: {0}", response);
        return response;
    }

    private static String toUpperCase(byte[] name) {
        byte[] upper = new byte[name.length];
        for (int i = 0; i < name.length; i++) {
            byte b = name[i];
            upper[i] = (b >= 'a' && b <= 'z') ? (byte) (b - 32) : b;
        }
        return new String(upper, StandardCharsets.UTF_8);
    }
}
