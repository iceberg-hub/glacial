package org.iceberg.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Store {

    record Entry(byte[] value, long expiresAtMillis) {}

    public static final long NO_EXPIRY = Long.MAX_VALUE;

    private final ConcurrentHashMap<String, Entry> data = new ConcurrentHashMap<>();
    private static final int ACTIVE_EXPIRY_SAMPLE_SIZE = 20;

    public Store() {
        startActiveExpiryThread();
    }

    public void set(String key, byte[] value) {
        set(key, value, NO_EXPIRY);
    }

    public void set(String key, byte[] value, long expiresAtMillis) {
        data.put(key, new Entry(value, expiresAtMillis));
    }

    public byte[] get(String key) {
        var entry = data.get(key);
        if (entry == null) {
            return null;
        }
        if (isExpired(entry)) {
            data.remove(key);
            return null;
        }
        return entry.value;
    }

    public long removeExpiredEntries() {
        long now = System.currentTimeMillis();
        long removed = 0;
        var candidates = new ArrayList<String>();
        var keys = data.keySet().iterator();
        int sampled = 0;
        while (keys.hasNext() && sampled < ACTIVE_EXPIRY_SAMPLE_SIZE) {
            candidates.add(keys.next());
            sampled++;
        }
        for (var key : candidates) {
            var entry = data.get(key);
            if (entry != null && isExpired(entry, now)) {
                data.remove(key);
                removed++;
            }
        }
        return removed;
    }

    private boolean isExpired(Entry entry) {
        return isExpired(entry, System.currentTimeMillis());
    }

    private boolean isExpired(Entry entry, long now) {
        return entry.expiresAtMillis != NO_EXPIRY && now >= entry.expiresAtMillis;
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
}
