plugins {
    kotlin("jvm") version "1.9.22"
}

group = "org.vivlaniv.nexohub"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.redisson:redisson:3.21.3")
    implementation("com.ecwid.clickhouse:clickhouse-client:0.9.0")
    implementation("com.clickhouse:clickhouse-jdbc:0.4.6")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}