package org.iceberg.server.command;

import org.iceberg.resp.BulkString;
import org.iceberg.resp.RespError;
import org.iceberg.resp.SimpleString;
import org.iceberg.server.Persistence;
import org.iceberg.server.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SaveCommandTest {

    @TempDir
    Path tempDir;

    private Store store;
    private SaveCommand command;
    private Path savePath;

    @BeforeEach
    void setUp() {
        store = new Store();
        savePath = tempDir.resolve("dump.rdb");
        command = new SaveCommand(store, savePath);
    }

    @Test
    void saveEmptyStore() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("SAVE".getBytes())
        });
        assertInstanceOf(SimpleString.class, result);
        assertEquals("OK", ((SimpleString) result).value());
        assertTrue(java.nio.file.Files.exists(savePath));
    }

    @Test
    void saveWithDataAndVerify() {
        store.set("key", "value".getBytes());
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("SAVE".getBytes())
        });
        assertInstanceOf(SimpleString.class, result);

        var newStore = new Store();
        Persistence.load(newStore, savePath);
        assertArrayEquals("value".getBytes(), newStore.get("key"));
    }

    @Test
    void errorWhenTooManyArgs() {
        var result = command.execute(new org.iceberg.resp.RespValue[]{
                new BulkString("SAVE".getBytes()),
                new BulkString("extra".getBytes())
        });
        assertInstanceOf(RespError.class, result);
        assertTrue(((RespError) result).value().contains("wrong number of arguments"));
    }
}
