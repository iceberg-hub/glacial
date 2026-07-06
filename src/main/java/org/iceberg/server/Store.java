package org.iceberg.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Store {
    private final Map<String, byte[]> data = new ConcurrentHashMap<>();

    public void set(String key, byte[] value) {
        data.put(key, value);
    }

    public byte[] get(String key) {
        return data.get(key);
    }
}
