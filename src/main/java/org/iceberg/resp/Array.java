package org.iceberg.resp;

import java.util.Arrays;

public record Array(RespValue[] value) implements RespValue {

    public static final Array NULL = new Array(null);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Array array = (Array) o;
        return Arrays.equals(value, array.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        if (value == null) return "Array(null)";
        return "Array(" + Arrays.toString(value) + ")";
    }
}
