/*
 * Copyright 2026 Rafael Oliveira
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    alias(libs.plugins.app.android.feature.data)
    alias(libs.plugins.kotlin.serialization)
}

// Cielo credentials (plan §4, Q12): Gradle properties from ~/.gradle/gradle.properties or -P, never versioned.
// The defaults are fictitious so the emulator and CI build without secrets. The token ends up inside the APK
// (accepted risk, plan §7); this is why it is also redacted from toString() and logs.
val cieloClientId = providers.gradleProperty("cielo.clientId").orElse("fake-client-id")
val cieloAccessToken = providers.gradleProperty("cielo.accessToken").orElse("fake-access-token")

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.rafaeloliveira.bilheteria.feature.ticketing.data"
    testOptions { unitTests.isIncludeAndroidResources = true }
    buildFeatures { buildConfig = true } // only CIELO_* below; no other module turns this on for credentials
    defaultConfig {
        buildConfigField("String", "CIELO_CLIENT_ID", cieloClientId.get().asBuildConfigString())
        buildConfigField("String", "CIELO_ACCESS_TOKEN", cieloAccessToken.get().asBuildConfigString())
    }
}

dependencies {
    implementation(projects.feature.ticketing.domain)
    implementation(projects.core.database)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core) // QR Code encoder (spec 002); pure Java, no camera, no permission
    implementation(libs.androidx.core.ktx) // NotificationCompat/ServiceCompat for the foreground service (minSdk 24)
    testImplementation(testFixtures(projects.feature.ticketing.domain))
    // Room in-memory tests need the Android framework (SQLite) on the JVM: Robolectric via the vintage engine.
    testImplementation(libs.robolectric)
    testImplementation(libs.junit.vintage.engine) // brings junit 4 (@Test, @RunWith) to the compile classpath
}

// BundledCatalogAssetTest reads the shipped asset from outside src/test: declare it so the test re-runs when it changes.
tasks.withType<Test>().configureEach {
    inputs
        .file("src/main/assets/events.json")
        .withPropertyName("eventsAsset")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
