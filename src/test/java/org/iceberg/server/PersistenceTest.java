package org.iceberg.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {

    @TempDir
    Path tempDir;

    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store();
    }

    @Test
    void saveAndLoadBytesValue() throws IOException {
        store.set("key1", "value1".getBytes());
        store.set("key2", "value2".getBytes());
        var path = tempDir.resolve("dump.rdb");
        Persistence.save(store, path);

        assertTrue(Files.exists(path));

        var newStore = new Store();
        Persistence.load(newStore, path);
        assertArrayEquals("value1".getBytes(), newStore.get("key1"));
        assertArrayEquals("value2".getBytes(), newStore.get("key2"));
    }

    @Test
    void saveAndLoadListValue() throws IOException {
        store.lpush("mylist", "a".getBytes(), "b".getBytes(), "c".getBytes());
        var path = tempDir.resolve("dump.rdb");
        Persistence.save(store, path);

        var newStore = new Store();
        Persistence.load(newStore, path);
        var list = newStore.lrange("mylist", 0, -1);
        assertEquals(3, list.size());
        assertArrayEquals("c".getBytes(), list.get(0));
        assertArrayEquals("b".getBytes(), list.get(1));
        assertArrayEquals("a".getBytes(), list.get(2));
    }

    @Test
    void saveAndLoadMixedTypes() throws IOException {
        store.set("strkey", "hello".getBytes());
        store.lpush("listkey", "x".getBytes(), "y".getBytes());
        var path = tempDir.resolve("dump.rdb");
        Persistence.save(store, path);

        var newStore = new Store();
        Persistence.load(newStore, path);
        assertArrayEquals("hello".getBytes(), newStore.get("strkey"));
        var list = newStore.lrange("listkey", 0, -1);
        assertEquals(2, list.size());
        assertArrayEquals("y".getBytes(), list.get(0));
        assertArrayEquals("x".getBytes(), list.get(1));
    }

    @Test
    void loadNonExistentFileDoesNotFail() {
        var path = tempDir.resolve("nonexistent.rdb");
        Persistence.load(store, path);
        assertEquals(0, store.entries().size());
    }

    @Test
    void saveOverwritesExistingFile() throws IOException {
        store.set("key", "old".getBytes());
        var path = tempDir.resolve("dump.rdb");
        Persistence.save(store, path);

        store.set("key", "new".getBytes());
        Persistence.save(store, path);

        var newStore = new Store();
        Persistence.load(newStore, path);
        assertArrayEquals("new".getBytes(), newStore.get("key"));
    }
}
