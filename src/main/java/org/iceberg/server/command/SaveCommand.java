package org.iceberg.server.command;

import org.iceberg.resp.RespError;
import org.iceberg.resp.RespValue;
import org.iceberg.resp.SimpleString;
import org.iceberg.server.Persistence;
import org.iceberg.server.Store;

import java.io.IOException;
import java.nio.file.Path;

public class SaveCommand implements Command {
    private final Store store;
    private final Path savePath;

    public SaveCommand(Store store, Path savePath) {
        this.store = store;
        this.savePath = savePath;
    }

    @Override
    public RespValue execute(RespValue[] args) {
        if (args.length != 1) {
            return new RespError("ERR wrong number of arguments for 'save' command");
        }
        try {
            Persistence.save(store, savePath);
            return new SimpleString("OK");
        } catch (IOException e) {
            return new RespError("ERR failed to save: " + e.getMessage());
        }
    }
}
