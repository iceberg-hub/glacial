package org.iceberg.server;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Store {

    public static final long NO_EXPIRY = Long.MAX_VALUE;

    private final Map<String, StoreValue> data = new ConcurrentHashMap<>();
    private static final int ACTIVE_EXPIRY_SAMPLE_SIZE = 20;

    public Store() {
        startActiveExpiryThread();
    }

    public void set(String key, byte[] value) {
        set(key, value, NO_EXPIRY);
    }

    public void set(String key, byte[] value, long expiresAtMillis) {
        data.put(key, new StoreValue.BytesValue(value, expiresAtMillis));
    }

    public byte[] get(String key) {
        var val = data.get(key);
        if (val instanceof StoreValue.BytesValue(byte[] bytes, long expiresAtMillis)) {
            if (isExpired(expiresAtMillis)) {
                data.remove(key);
                return null;
            }
            return bytes;
        }
        return null;
    }

    public boolean exists(String key) {
        var val = data.get(key);
        if (val instanceof StoreValue.BytesValue(byte[] bytes, long expiresAtMillis)) {
            if (isExpired(expiresAtMillis)) {
                data.remove(key);
                return false;
            }
            return true;
        }
        return val != null;
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
        long[] result = new long[1];
        data.compute(key, (k, val) -> {
            result[0] = resolveNumericValue(val) + 1;
            return new StoreValue.BytesValue(encodeLong(result[0]));
        });
        return result[0];
    }

    public long decrement(String key) {
        long[] result = new long[1];
        data.compute(key, (k, val) -> {
            result[0] = resolveNumericValue(val) - 1;
            return new StoreValue.BytesValue(encodeLong(result[0]));
        });
        return result[0];
    }

    public void lpush(String key, byte[]... values) {
        var list = getOrCreateList(key);
        for (byte[] value : values) {
            list.elements().add(0, value);
        }
    }

    public void rpush(String key, byte[]... values) {
        var list = getOrCreateList(key);
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

    public Map<String, StoreValue> entries() {
        return Collections.unmodifiableMap(data);
    }

    public void loadFrom(Map<String, StoreValue> entries) {
        data.clear();
        data.putAll(entries);
    }

    private long resolveNumericValue(StoreValue val) {
        if (val == null) {
            return 0;
        }
        if (val instanceof StoreValue.BytesValue(byte[] bytes, long expiresAtMillis)) {
            if (isExpired(expiresAtMillis)) {
                return 0;
            }
            return Long.parseLong(new String(bytes, StandardCharsets.UTF_8));
        }
        throw new StoreTypeException("ERR wrong type or key");
    }

    private StoreValue.ListValue getOrCreateList(String key) {
        var existing = data.get(key);
        if (existing instanceof StoreValue.ListValue lv) {
            return lv;
        }
        if (existing != null) {
            throw new StoreTypeException("ERR wrong type for key");
        }
        var newList = new StoreValue.ListValue();
        var raceWinner = data.putIfAbsent(key, newList);
        if (raceWinner instanceof StoreValue.ListValue lv) {
            return lv;
        }
        if (raceWinner != null) {
            throw new StoreTypeException("ERR wrong type for key");
        }
        return newList;
    }

    private int normalizeIndex(long index, int size) {
        if (index < 0) {
            return Math.max(0, size + (int) index);
        }
        return (int) Math.min(index, size);
    }

    private boolean isExpired(long expiresAtMillis) {
        return expiresAtMillis != NO_EXPIRY && System.currentTimeMillis() >= expiresAtMillis;
    }

    private void removeExpiredEntries() {
        long now = System.currentTimeMillis();
        var keys = data.keySet().iterator();
        int sampled = 0;
        while (keys.hasNext() && sampled < ACTIVE_EXPIRY_SAMPLE_SIZE) {
            var key = keys.next();
            sampled++;
            var val = data.get(key);
            if (val instanceof StoreValue.BytesValue(byte[] bytes, long expiresAtMillis)) {
                if (expiresAtMillis != NO_EXPIRY && now >= expiresAtMillis) {
                    data.remove(key);
                }
            }
        }
    }

    private void startActiveExpiryThread() {
        Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(100);
                    removeExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private static byte[] encodeLong(long value) {
        return Long.toString(value).getBytes(StandardCharsets.UTF_8);
    }

    public static class StoreTypeException extends RuntimeException {
        public StoreTypeException(String message) {
            super(message);
        }
    }
}
