import java.util.Properties

plugins {
    // AGP 9.0+ has Kotlin support built in. The separate 'org.jetbrains.kotlin.android' plugin is
    // not only unnecessary now, it is rejected — AGP fails the build if you apply it. Most
    // Android build guidance still shows it, so this is worth a comment rather than a silent
    // omission. See https://kotl.in/gradle/agp-built-in-kotlin
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Everything secret this build needs, read from local.properties (git-ignored) or the
 * environment. Never a literal in a committed file: this repository is public for the
 * hackathon's Next Gen track, and anything checked in here is leaked.
 */
val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(property: String, environment: String): String? =
    localProperties.getProperty(property) ?: System.getenv(environment)

/**
 * The RevenueCat key. Blank is a supported state — the app falls back to the placeholder paywall
 * rather than failing to build, so anyone can clone and run it without a RevenueCat account.
 */
val revenueCatApiKey: String = secret("revenuecat.apiKey", "REVENUECAT_API_KEY") ?: ""

/**
 * The release keystore, from local.properties (git-ignored) or the environment.
 *
 * Absent is a supported state, for the same reason the key above may be blank: a clone without
 * the keystore still builds, and produces an unsigned release APK rather than failing. What it
 * must never be is committed — anyone holding the keystore and its password can sign a build
 * that claims to be this app, so both stay outside the repository and .gitignore enforces it.
 */
val releaseKeystore: File? = secret("signing.storeFile", "EPLEY_STORE_FILE")
    ?.let { project.file(it) }
    ?.takeIf { it.exists() }

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

        buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatApiKey\"")
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = secret("signing.storePassword", "EPLEY_STORE_PASSWORD")
                keyAlias = secret("signing.keyAlias", "EPLEY_KEY_ALIAS")
                keyPassword = secret("signing.keyPassword", "EPLEY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Null when no keystore is configured, which leaves the APK unsigned rather than
            // failing the build. An unsigned APK will not install, and that is the honest
            // outcome for a checkout that has no key.
            signingConfig = signingConfigs.findByName("release")
            // R8 is off for now. It will be turned on once there is a release build to ship, with
            // keep rules verified against a real device — silently stripped classes are a classic
            // way for a release build to break in ways debug never shows.
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        // Off by default since AGP 8. Needed for REVENUECAT_API_KEY above.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            // Screenshot tests (README ★P3) write their PNGs straight to disk for tools/compare.mjs.
            it.systemProperty("roborazzi.test.record", "true")
            it.systemProperty("parity.device", System.getProperty("parity.device") ?: "")
            it.systemProperty("parity.only", System.getProperty("parity.only") ?: "")
            it.systemProperty("parity.pdf", System.getProperty("parity.pdf") ?: "")
            it.maxHeapSize = "3g"
            // Robolectric reaches into FileDescriptor internals; JDK 24 no longer exports them.
            it.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
            )
        }
        // Fonts, assets and the held figure frames have to be visible to Robolectric.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    // Icons are load-bearing here, not decoration: every state is signalled by a word, an icon, a
    // shape and a colour, so that none of the four is doing the job alone.
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.13.0")
    // The system splash, so the launcher icon hands over to the app without a blank frame.
    // The platform attributes alone cannot do this: postSplashScreenTheme is an androidx
    // attribute, and keeping the splash up while the figure model loads needs the API.
    implementation("androidx.core:core-splashscreen:1.0.1")
    // The 3D figure runs the design's own three.js renderer from app assets, offline, through
    // WebViewAssetLoader (README §7). Never a remodel.
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Purchases only. The paywall stands in front of one non-clinical feature, so this dependency
    // must never be reachable from the manoeuvre, the safety check or the questionnaire.
    implementation("com.revenuecat.purchases:purchases:10.22.1")

    // RunController is the bridge between the sensor stream and the engine, and it is the part
    // that cannot be reached on a device without someone physically holding the phone against
    // their head. It has to be provable on the JVM instead.
    // Explicit coordinates rather than kotlin("test"): the Android plugin does not supply a
    // version for that helper the way the JVM plugin does, and the dependency silently resolves
    // to nothing.
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.20")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    // Pixel parity with the design (README ★P3): Robolectric native graphics + Roborazzi, JUnit 4
    // tests run next to the JUnit 5 ones through the vintage engine.
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.76.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.76.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
