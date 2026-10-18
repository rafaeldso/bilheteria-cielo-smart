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
    `kotlin-dsl`
}

group = "com.rafaeloliveira.bilheteria.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Plugins aplicados no build raiz com `apply false`: aqui só compileOnly (padrão Now in Android).
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    // Sensores: aplicados só pelos convention plugins.
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.moduleGraphAssertion.gradlePlugin)
    implementation(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("root") {
            id = "app.root"
            implementationClass = "RootConventionPlugin"
        }
        register("quality") {
            id = "app.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("androidApplication") {
            id = "app.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "app.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "app.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeatureUi") {
            id = "app.android.feature.ui"
            implementationClass = "AndroidFeatureUiConventionPlugin"
        }
        register("androidFeatureData") {
            id = "app.android.feature.data"
            implementationClass = "AndroidFeatureDataConventionPlugin"
        }
        register("jvmLibrary") {
            id = "app.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidRoom") {
            id = "app.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("hilt") {
            id = "app.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
