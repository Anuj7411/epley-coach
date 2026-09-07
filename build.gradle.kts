plugins {
    // AGP 9 bundles Kotlin for Android modules, so no kotlin("android") here.
    // The JVM plugin is still needed for :core, which is plain Kotlin with no Android dependency
    // — that separation is what lets the angle maths be unit-tested without a device.
    id("com.android.application") version "9.4.0" apply false
    kotlin("jvm") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
