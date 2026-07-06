package org.iceberg;

import org.iceberg.benchmark.BenchmarkConfig;
import org.iceberg.benchmark.BenchmarkRunner;
import org.iceberg.server.RedisServer;
import org.iceberg.server.async.AsyncRedisServer;

import java.util.Locale;

public class Main {
    static void main(String[] args) throws Exception {
        if (args.length > 0 && "benchmark".equals(args[0])) {
            var remaining = new String[args.length - 1];
            System.arraycopy(args, 1, remaining, 0, remaining.length);
            var config = BenchmarkConfig.parse(remaining);
            new BenchmarkRunner(config).run();
            return;
        }

        var port = 6379;
        var mode = "threaded";

        for (var i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port", "-p" -> port = Integer.parseInt(args[++i]);
                case "--mode", "-m" -> mode = args[++i];
            }
        }

        switch (mode.toLowerCase(Locale.ROOT)) {
            case "threaded", "thread" -> {
                var server = new RedisServer(port);
                server.start();
            }
            case "async" -> {
                var server = new AsyncRedisServer(port);
                server.start();
            }
            default -> {
                System.err.println("Unknown mode: " + mode);
                System.err.println("Usage: glacial [--port <port>] [--mode threaded|async]");
                System.err.println("       glacial benchmark [-m threaded|async] [-n <requests>] [-c <clients>] [-q]");
                System.exit(1);
            }
        }
    }
}
