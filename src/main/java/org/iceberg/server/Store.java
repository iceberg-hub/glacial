package org.iceberg.server;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Store {
    private final Map<String, StoreValue> data = new ConcurrentHashMap<>();

    public void set(String key, byte[] value) {
        data.put(key, new StoreValue.BytesValue(value));
    }

    public byte[] get(String key) {
        var val = data.get(key);
        if (val instanceof StoreValue.BytesValue(byte[] bytes)) {
            return bytes;
        }
        return null;
    }

    public boolean exists(String key) {
        return data.containsKey(key);
    }

    public int delete(String... keys) {
        int count = 0;
        for (String key : keys) {
            if (data.remove(key) != null) {
                count++;
            }
        }
        return count;
    }

    public long increment(String key) {
        var val = data.get(key);
        long current = 0;
        if (val != null) {
            if (!(val instanceof StoreValue.BytesValue(byte[] bytes))) {
                throw new StoreTypeException("ERR wrong type or key");
            }
            current = Long.parseLong(new String(bytes, StandardCharsets.UTF_8));
        }
        long result = current + 1;
        data.put(key, new StoreValue.BytesValue(Long.toString(result).getBytes(StandardCharsets.UTF_8)));
        return result;
    }

    public long decrement(String key) {
        var val = data.get(key);
        long current = 0;
        if (val != null) {
            if (!(val instanceof StoreValue.BytesValue(byte[] bytes))) {
                throw new StoreTypeException("ERR wrong type or key");
            }
            current = Long.parseLong(new String(bytes, StandardCharsets.UTF_8));
        }
        long result = current - 1;
        data.put(key, new StoreValue.BytesValue(Long.toString(result).getBytes(StandardCharsets.UTF_8)));
        return result;
    }

    private StoreValue.ListValue getListOrCreate(String key) {
        var existing = data.get(key);
        if (existing instanceof StoreValue.ListValue lv) {
            return lv;
        } else if (existing == null) {
            var newList = new StoreValue.ListValue();
            var existing2 = data.putIfAbsent(key, newList);
            if (existing2 instanceof StoreValue.ListValue lv) {
                return lv;
            }
            return newList;
        } else {
            throw new StoreTypeException("ERR wrong type for key");
        }
    }

    public void lpush(String key, byte[]... values) {
        var list = getListOrCreate(key);
        for (byte[] value : values) {
            list.elements().add(0, value);
        }
    }

    public void rpush(String key, byte[]... values) {
        var list = getListOrCreate(key);
        for (byte[] value : values) {
            list.elements().add(value);
        }
    }

    public int llen(String key) {
        var val = data.get(key);
        if (val instanceof StoreValue.ListValue(java.util.concurrent.CopyOnWriteArrayList<byte[]> list)) {
            return list.size();
        }
        return -1;
    }

    public List<byte[]> lrange(String key, long start, long stop) {
        var val = data.get(key);
        if (val instanceof StoreValue.ListValue(java.util.concurrent.CopyOnWriteArrayList<byte[]> list)) {
            int size = list.size();
            int normalizedStart = normalizeIndex(start, size);
            int normalizedStop = normalizeIndex(stop, size);
            if (normalizedStart > normalizedStop || normalizedStart >= size) {
                return Collections.emptyList();
            }
            normalizedStop = Math.min(normalizedStop, size - 1);
            return new ArrayList<>(list.subList(normalizedStart, normalizedStop + 1));
        }
        return Collections.emptyList();
    }

    private int normalizeIndex(long index, int size) {
        if (index < 0) {
            return Math.max(0, size + (int) index);
        }
        return (int) Math.min(index, size);
    }

    public Map<String, StoreValue> entries() {
        return Collections.unmodifiableMap(data);
    }

    public void loadFrom(Map<String, StoreValue> entries) {
        data.clear();
        data.putAll(entries);
    }

    public static class StoreTypeException extends RuntimeException {
        public StoreTypeException(String message) {
            super(message);
        }
    }
}
