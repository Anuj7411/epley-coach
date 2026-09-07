plugins {
    // AGP 9.0+ has Kotlin support built in. The separate 'org.jetbrains.kotlin.android' plugin is
    // not only unnecessary now, it is rejected — AGP fails the build if you apply it. Most
    // Android build guidance still shows it, so this is worth a comment rather than a silent
    // omission. See https://kotl.in/gradle/agp-built-in-kotlin
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "health.epley.app"
    // Compose BOM 2026.08 requires compiling against API 37. Note this is separate
    // from targetSdk (runtime behaviour) and minSdk (device coverage).
    compileSdk = 37

    defaultConfig {
        applicationId = "health.epley.app"
        // API 24 covers essentially every device still in use and is the floor for the sensor
        // APIs we need. Nothing here requires anything newer.
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-probe"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // R8 is off for now. It will be turned on once there is a release build to ship, with
            // keep rules verified against a real device — silently stripped classes are a classic
            // way for a release build to break in ways debug never shows.
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // RunController is the bridge between the sensor stream and the engine, and it is the part
    // that cannot be reached on a device without someone physically holding the phone against
    // their head. It has to be provable on the JVM instead.
    // Explicit coordinates rather than kotlin("test"): the Android plugin does not supply a
    // version for that helper the way the JVM plugin does, and the dependency silently resolves
    // to nothing.
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.20")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
