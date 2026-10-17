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
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Per-module sensors (id: app.quality), applied by the other convention plugins:
 * detekt + project rules (S-04/S-05) and Kover with per-layer thresholds (S-11, testing.md §8).
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureDetekt()
            configureKover()
        }
    }

    private fun Project.configureDetekt() {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        extensions.configure<DetektExtension> {
            config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
            parallel = true
        }
        dependencies {
            // detekt-rules also analyzes itself with its own jar (detekt needs it to validate `project-rules`).
            add("detektPlugins", project(DETEKT_RULES))
            add("detektPlugins", libs.library("composeRules-detekt"))
        }
        // detekt 1.23.x runs on its own Kotlin compiler; keep the project's Kotlin off its classpath.
        configurations.matching { it.name == "detekt" || it.name == "detektPlugins" }.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") useVersion(getSupportedKotlinVersion())
            }
        }
    }

    private fun Project.configureKover() {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        // Instrumentation slows unit tests: only on harnessFull/kover runs or -Pharness.coverage=true.
        val requested = gradle.startParameter.taskNames.any { "harnessFull" in it || "kover" in it }
        val coverage = requested || providers.gradleProperty("harness.coverage").orNull.toBoolean()
        val minimum = LAYER_COVERAGE.entries.firstOrNull { path.endsWith(":${it.key}") }?.value
        extensions.configure<KoverProjectExtension> {
            currentProject {
                instrumentation { disabledForAll.set(!coverage) }
            }
            if (minimum != null) {
                reports { verify { rule { minBound(minimum) } } }
            }
        }
    }

    private companion object {
        const val DETEKT_RULES = ":quality:detekt-rules"
        val LAYER_COVERAGE = mapOf("domain" to 90, "ui" to 85, "data" to 75)
    }
}
