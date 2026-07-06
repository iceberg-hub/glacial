plugins {
    id("java")
    application
}

application {
    mainClass = "org.iceberg.Main"
}

group = "org.iceberg"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.lettuce:lettuce-core:7.6.0.RELEASE")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("benchmark") {
    group = "benchmark"
    description = "Run the Glacial benchmark"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.iceberg.benchmark.Benchmark"
    jvmArgs = listOf("-Djava.util.logging.config.file=/dev/null")
    args = listOf("-m", "threaded", "-n", "10000", "-c", "50", "-q")
}

tasks.register<JavaExec>("benchmarkAsync") {
    group = "benchmark"
    description = "Run the Glacial benchmark (async mode)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.iceberg.benchmark.Benchmark"
    jvmArgs = listOf("-Djava.util.logging.config.file=/dev/null")
    args = listOf("-m", "async", "-n", "10000", "-c", "50", "-q")
}