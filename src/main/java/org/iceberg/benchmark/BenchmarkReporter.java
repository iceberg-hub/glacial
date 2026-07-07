package org.iceberg.benchmark;

import java.util.List;

public class BenchmarkReporter {

    public String format(String command, MetricsCollector metrics, long elapsedNs, boolean quiet) {
        var count = metrics.count();
        var latencies = metrics.sortedLatencies();
        var elapsedSec = elapsedNs / 1_000_000_000.0;
        var throughput = elapsedSec > 0 ? count / elapsedSec : 0;
        var p50us = latencies.isEmpty() ? 0 : latencies.get(latencies.size() / 2) / 1000;
        var p50ms = p50us / 1000.0;

        if (quiet) {
            return String.format("%s: %.0f requests per second, p50=%.3f msec", command, throughput, p50ms);
        }
        return String.format("%s: %.0f requests per second, p50=%.3f msec (n=%d)", command, throughput, p50ms, count);
    }

    public void report(String command, MetricsCollector metrics, long elapsedNs, boolean quiet) {
        System.out.println(format(command, metrics, elapsedNs, quiet));
    }
}
