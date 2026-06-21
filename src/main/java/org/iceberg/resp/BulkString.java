package org.iceberg.resp;

import java.util.Arrays;

public record BulkString(byte[] value) implements RespValue {

    public static final BulkString NULL = new BulkString(null);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkString that = (BulkString) o;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        if (value == null) return "BulkString(null)";
        return "BulkString(\"" + new String(value) + "\")";
    }
}
