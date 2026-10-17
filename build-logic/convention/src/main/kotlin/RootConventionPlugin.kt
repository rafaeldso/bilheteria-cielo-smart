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
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applied once on the root project (id: app.root). Owns Spotless (S-03) and the stable task
 * contract used by scripts/harness/verify.sh:
 *   harnessFast  -> spotlessCheck, detekt, architecture tests
 *   harnessCheck -> harnessFast + unit tests + Android Lint
 *   harnessFull  -> harnessCheck + Kover + module graph + release build
 * Roborazzi is out of scope (ADR-0009, S8). Each sensor is wired lazily via withPlugin.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) { "app.root must be applied to the root project" }
        with(target) {
            configureSpotless()

            val fast =
                tasks.register("harnessFast") {
                    group = HARNESS
                    description = "Formatação, detekt, arquitetura"
                    dependsOn("spotlessCheck")
                }
            val check =
                tasks.register("harnessCheck") {
                    group = HARNESS
                    description = "harnessFast + testes unitários + Android Lint"
                    dependsOn(fast)
                }
            val full =
                tasks.register("harnessFull") {
                    group = HARNESS
                    description = "harnessCheck + cobertura + grafo de módulos + build release"
                    dependsOn(check)
                }

            subprojects {
                val module = this
                pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
                    fast.configure { dependsOn(module.tasks.named("detekt")) }
                }
                pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
                    full.configure { dependsOn(module.tasks.named("koverVerify")) }
                }
                pluginManager.withPlugin("com.android.library") {
                    check.configure {
                        dependsOn(module.tasks.named("testDebugUnitTest"), module.tasks.named("lintDebug"))
                    }
                }
                pluginManager.withPlugin("com.android.application") {
                    check.configure {
                        dependsOn(module.tasks.named("testDebugUnitTest"), module.tasks.named("lintDebug"))
                    }
                    full.configure { dependsOn(module.tasks.named("assembleRelease")) }
                }
                pluginManager.withPlugin("com.jraska.module.graph.assertion") {
                    full.configure { dependsOn(module.tasks.named("assertModuleGraph")) }
                }
                pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    val owner = if (module.path == ARCHITECTURE_TESTS) fast else check
                    owner.configure { dependsOn(module.tasks.named("test")) }
                }
            }
        }
    }

    private fun Project.configureSpotless() {
        pluginManager.apply("com.diffplug.spotless")
        val header = rootDir.resolve("docs/harness/templates/license-header.txt")
        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**", "docs/**")
                ktlint()
                licenseHeaderFile(header)
            }
            kotlinGradle {
                target("**/*.kts")
                targetExclude("**/build/**")
                ktlint()
                // First line that is not part of the /* ... */ header.
                licenseHeaderFile(header, "(?!/\\*| \\*).+")
            }
        }
    }

    private companion object {
        const val HARNESS = "harness"
        const val ARCHITECTURE_TESTS = ":quality:architecture-tests"
    }
}
