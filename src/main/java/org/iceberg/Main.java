package org.iceberg;

import org.iceberg.server.RedisServer;

public class Main {
    static void main(String[] args) {
        var server = new RedisServer(6379);
        server.start();
    }
}
