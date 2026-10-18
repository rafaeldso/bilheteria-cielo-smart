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
import com.android.build.api.dsl.LibraryExtension
import com.rafaeloliveira.bilheteria.buildlogic.library
import com.rafaeloliveira.bilheteria.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** feature:*:ui (id: app.android.feature.ui): library + Compose + Hilt + design system. */
class AndroidFeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.android.library")
            pluginManager.apply("app.android.compose")
            pluginManager.apply("app.hilt")
            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-navigation-compose"))
                add("implementation", libs.library("androidx-hilt-navigation-compose"))
                add("implementation", libs.library("kotlinx-collections-immutable"))
                add("testImplementation", project(":core:testing"))
                // Screen tests run on the JVM with Robolectric (JUnit 4 rule via the vintage engine; testing.md).
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
                add("testRuntimeOnly", libs.library("junit-vintage-engine"))
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            }
            extensions.configure<LibraryExtension> {
                testOptions { unitTests.isIncludeAndroidResources = true }
            }
        }
    }
}

/** feature:*:data (id: app.android.feature.data): library + Hilt. Network/database enter with the first feature that needs them. */
class AndroidFeatureDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.android.library")
            pluginManager.apply("app.hilt")
            dependencies {
                add("implementation", project(":core:common"))
                add("testImplementation", project(":core:testing"))
            }
        }
    }
}
