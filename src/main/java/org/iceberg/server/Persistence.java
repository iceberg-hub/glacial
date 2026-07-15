package org.iceberg.server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Persistence {

    private static final Logger LOG = Logger.getLogger(Persistence.class.getName());

    public static void save(Store store, Path path) throws IOException {
        var tmpPath = path.resolveSibling(path.getFileName() + ".tmp");
        try (var out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpPath)))) {
            var entries = store.entries();
            out.writeInt(entries.size());
            for (var entry : entries.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                out.writeUTF(key);
                if (value instanceof StoreValue.BytesValue(byte[] data, long expiresAtMillis)) {
                    out.writeByte('B');
                    out.writeInt(data.length);
                    out.write(data);
                } else if (value instanceof StoreValue.ListValue(java.util.concurrent.CopyOnWriteArrayList<byte[]> elements)) {
                    out.writeByte('L');
                    out.writeInt(elements.size());
                    for (byte[] element : elements) {
                        out.writeInt(element.length);
                        out.write(element);
                    }
                }
            }
        }
        Files.move(tmpPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    public static void load(Store store, Path path) {
        if (!Files.exists(path)) {
            LOG.log(Level.INFO, "No dump file found at {0}, starting with empty database", path);
            return;
        }
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int count = in.readInt();
            Map<String, StoreValue> entries = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String key = in.readUTF();
                byte type = in.readByte();
                switch (type) {
                    case 'B' -> {
                        int len = in.readInt();
                        byte[] data = new byte[len];
                        in.readFully(data);
                        entries.put(key, new StoreValue.BytesValue(data));
                    }
                    case 'L' -> {
                        int listLen = in.readInt();
                        var list = new java.util.concurrent.CopyOnWriteArrayList<byte[]>();
                        for (int j = 0; j < listLen; j++) {
                            int elemLen = in.readInt();
                            byte[] elem = new byte[elemLen];
                            in.readFully(elem);
                            list.add(elem);
                        }
                        entries.put(key, new StoreValue.ListValue(list));
                    }
                    default -> throw new IOException("Unknown value type: " + (char) type);
                }
            }
            store.loadFrom(entries);
            LOG.log(Level.INFO, "Loaded {0} keys from dump file", count);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load dump file", e);
        }
    }
}
