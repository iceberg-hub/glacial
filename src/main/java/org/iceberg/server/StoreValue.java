package org.iceberg.server;

import java.util.List;

public sealed interface StoreValue {
    record BytesValue(byte[] data) implements StoreValue {}
    record ListValue(java.util.concurrent.CopyOnWriteArrayList<byte[]> elements) implements StoreValue {
        public ListValue() {
            this(new java.util.concurrent.CopyOnWriteArrayList<>());
        }
    }
}
