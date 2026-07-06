package org.iceberg.benchmark;

import org.iceberg.server.RedisServer;
import org.iceberg.server.async.AsyncRedisServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BenchmarkRunner {

    private final BenchmarkConfig config;

    public BenchmarkRunner(BenchmarkConfig config) {
        this.config = config;
    }

    public void run() throws Exception {
        var serverThread = startServer();
        waitForPort(config.port(), Duration.ofSeconds(5));
        var result = runWorkload();
        serverThread.interrupt();
        System.out.print(result);
    }

    public String runWorkload() throws Exception {
        var setMetrics = new MetricsCollector();
        var getMetrics = new MetricsCollector();
        var requestsPerClient = Math.max(1, config.requests() / config.clients());
        var startTime = System.nanoTime();
        var executor = Executors.newFixedThreadPool(config.clients());

        for (var c = 0; c < config.clients(); c++) {
            var clientId = c;
            executor.submit(() -> {
                try (var client = new RespClient("localhost", config.port(), BenchmarkConfig.CONNECT_TIMEOUT_MS)) {
                    runClient(client, clientId, requestsPerClient, setMetrics, getMetrics);
                } catch (IOException e) {
                    setMetrics.error();
                }
                return null;
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        var elapsedNs = System.nanoTime() - startTime;

        var reporter = new BenchmarkReporter();
        var sb = new StringBuilder();
        sb.append(reporter.format("SET", setMetrics, elapsedNs, config.quiet())).append('\n');
        sb.append(reporter.format("GET", getMetrics, elapsedNs, config.quiet())).append('\n');

        if (!config.quiet()) {
            var totalErrors = setMetrics.errors() + getMetrics.errors();
            if (totalErrors > 0) {
                sb.append("Errors: ").append(totalErrors).append('\n');
            }
        }
        return sb.toString();
    }

    private void runClient(RespClient client, int clientId, int requestsPerClient,
                           MetricsCollector setMetrics, MetricsCollector getMetrics) throws IOException {
        for (var r = 0; r < requestsPerClient; r++) {
            var key = "key:" + clientId + ":" + r;
            var value = "val:" + r;

            var setStart = System.nanoTime();
            client.sendCommand("SET", key, value);
            client.readStatus();
            setMetrics.record(System.nanoTime() - setStart);

            var getStart = System.nanoTime();
            client.sendCommand("GET", key);
            client.readBulkString();
            getMetrics.record(System.nanoTime() - getStart);
        }
    }

    private Thread startServer() {
        Runnable serverStarter = switch (config.mode()) {
            case "async" -> () -> {
                try {
                    new AsyncRedisServer(config.port()).start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            default -> () -> new RedisServer(config.port()).start();
        };

        var thread = new Thread(serverStarter);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void waitForPort(int port, Duration timeout) throws InterruptedException {
        var deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try (var s = new Socket()) {
                s.connect(new InetSocketAddress("localhost", port), 200);
                return;
            } catch (IOException ignored) {
                Thread.sleep(50);
            }
        }
        throw new RuntimeException("Server did not start on port " + port);
    }
}
