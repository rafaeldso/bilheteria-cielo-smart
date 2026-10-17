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
import com.rafaeloliveira.bilheteria.buildlogic.library
import com.rafaeloliveira.bilheteria.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Hilt with KSP (id: app.hilt); kapt is forbidden. JVM modules get hilt-core, Android modules hilt-android. */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            dependencies { add("ksp", libs.library("hilt-compiler")) }

            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies { add("implementation", libs.library("hilt-core")) }
            }
            listOf("com.android.application", "com.android.library").forEach { android ->
                pluginManager.withPlugin(android) {
                    pluginManager.apply("com.google.dagger.hilt.android")
                    dependencies { add("implementation", libs.library("hilt-android")) }
                }
            }
        }
    }
}
