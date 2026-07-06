plugins {
    id("java")
    application
}

application {
    mainClass = "org.iceberg.Main"
}

group = "org.iceberg"
version = "1.0-SNAPSHOT"

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