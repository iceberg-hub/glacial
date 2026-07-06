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
    implementation("io.lettuce:lettuce-core:6.4.2.RELEASE")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}