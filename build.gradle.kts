plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.23"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jetbrains.kotlinx.dataframe") version "0.13.1"
}

group = "ktex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotlinVersion = "2.0.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    implementation("org.jetbrains.kotlinx:dataframe:0.13.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    implementation("dev.cel:cel:0.4.1")
    implementation("org.openjdk.nashorn:nashorn-core:15.4")
    implementation(platform("com.google.cloud:libraries-bom:26.36.0"))
    implementation("com.google.cloud:google-cloud-functions")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.twineworks:tweakflow:1.+")
//    implementation("org.moirai-lang:moirai:0.3.5")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

    runtimeOnly("org.jetbrains.kotlin:kotlin-main-kts:$kotlinVersion")
    runtimeOnly("org.jetbrains.kotlin:kotlin-scripting-jsr223:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-main-kts:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-scripting-jsr223:$kotlinVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
}

tasks.test {
    useJUnitPlatform()

    systemProperties["junit.jupiter.execution.parallel.enabled"] = true
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

kotlin {
    jvmToolchain(17)
}