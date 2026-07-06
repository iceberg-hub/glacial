package org.iceberg.benchmark;

public class Benchmark {

    static void main(String[] args) throws Exception {
        var config = BenchmarkConfig.parse(args);
        new BenchmarkRunner(config).run();
    }
}
