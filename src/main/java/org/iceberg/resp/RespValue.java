package org.iceberg.resp;

public sealed interface RespValue
        permits SimpleString, RespError, RespInteger, BulkString, Array {
}
