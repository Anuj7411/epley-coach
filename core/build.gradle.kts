import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

repositories { mavenCentral() }

dependencies {
    testImplementation(kotlin("test"))
}

// Target Java 21 bytecode, but compile with whatever JDK is running.
//
// jvmToolchain(21) would be tidier, except it makes the build fail outright on a machine that has
// a newer JDK and no 21 — with no toolchain repository configured there is nothing for Gradle to
// download, and a build that cannot run on the only machine we have is worse than one pinned a
// little loosely. Target 21 still matters: it is what the Android module consumes, and bytecode
// newer than that would be rejected downstream.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
