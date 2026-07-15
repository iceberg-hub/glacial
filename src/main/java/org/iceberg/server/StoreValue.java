package org.iceberg.server;

public sealed interface StoreValue {
    record BytesValue(byte[] data, long expiresAtMillis) implements StoreValue {
        public BytesValue(byte[] data) {
            this(data, Long.MAX_VALUE);
        }
    }
    record ListValue(java.util.concurrent.CopyOnWriteArrayList<byte[]> elements) implements StoreValue {
        public ListValue() {
            this(new java.util.concurrent.CopyOnWriteArrayList<>());
        }
    }
}
