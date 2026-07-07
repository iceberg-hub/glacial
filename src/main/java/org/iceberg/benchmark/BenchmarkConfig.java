package org.iceberg.benchmark;

public record BenchmarkConfig(
    int port,
    int clients,
    int requests,
    String mode,
    boolean quiet
) {
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_CLIENTS = 50;
    public static final int DEFAULT_REQUESTS = 10000;
    public static final int CONNECT_TIMEOUT_MS = 5000;

    public static BenchmarkConfig parse(String[] args) {
        var port = DEFAULT_PORT;
        var clients = DEFAULT_CLIENTS;
        var requests = DEFAULT_REQUESTS;
        var mode = "threaded";
        var quiet = false;

        for (var i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p" -> port = Integer.parseInt(args[++i]);
                case "-c" -> clients = Integer.parseInt(args[++i]);
                case "-n" -> requests = Integer.parseInt(args[++i]);
                case "-q" -> quiet = true;
                case "-m" -> mode = args[++i];
            }
        }

        return new BenchmarkConfig(port, clients, requests, mode, quiet);
    }
}
