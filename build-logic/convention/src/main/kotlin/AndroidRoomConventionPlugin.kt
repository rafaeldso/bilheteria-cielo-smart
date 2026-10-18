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
import androidx.room.gradle.RoomExtension
import com.rafaeloliveira.bilheteria.buildlogic.library
import com.rafaeloliveira.bilheteria.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room with KSP and exported schema (id: app.android.room). Used by core:database (ADR-0007). */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")
            extensions.configure<RoomExtension> {
                // Versioned schemas make migrations reviewable and testable (architecture.md).
                schemaDirectory("$projectDir/schemas")
            }
            dependencies {
                add("implementation", libs.library("androidx-room-runtime"))
                add("ksp", libs.library("androidx-room-compiler"))
            }
        }
    }
}
