package org.iceberg.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector {

    private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
    private final AtomicInteger count = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();

    public void record(long nanos) {
        latencies.add(nanos);
        count.incrementAndGet();
    }

    public void error() {
        errorCount.incrementAndGet();
    }

    public int count() {
        return count.get();
    }

    public int errors() {
        return errorCount.get();
    }

    public List<Long> sortedLatencies() {
        var list = new ArrayList<Long>(latencies);
        Collections.sort(list);
        return list;
    }
}
