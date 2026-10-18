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
package com.rafaeloliveira.bilheteria.buildlogic

import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal const val JVM_TOOLCHAIN = 17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

/** Toolchain fixa e warnings como erro no CI (`-Pci=true`). Comum a módulos JVM e Android. */
internal fun Project.configureKotlin() {
    extensions.getByType<KotlinProjectExtension>().jvmToolchain(JVM_TOOLCHAIN)
    val ci = providers.gradleProperty("ci").map(String::toBoolean).orElse(false)
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(ci)
    }
}

internal fun CompileOptions.configureJava() {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    // java.time with minSdk 24 (ADR-0007: Instant/Clock in domain and data).
    isCoreLibraryDesugaringEnabled = true
}

internal fun Project.addDesugaring() {
    dependencies { add("coreLibraryDesugaring", libs.library("desugar-jdk-libs")) }
}

/** Sensor S-08: severidades do projeto em config/lint/lint.xml. */
internal fun Lint.configureLint(project: Project) {
    lintConfig = project.rootDir.resolve("config/lint/lint.xml")
    abortOnError = true
    checkDependencies = false
}

/** JUnit 5 + stack de testes unitários (testing.md). */
internal fun Project.configureUnitTests() {
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testImplementation", libs.library("mockk"))
        add("testImplementation", libs.library("turbine"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
    }
    // Gradle 9 fails a test task that finds classes but no tests. Android modules always put the
    // resources R.jar on the test classpath, so a module with no src/test would fail before its
    // first test exists. Modules with tests keep the check (it catches real misconfiguration).
    val hasTestSources =
        layout.projectDirectory
            .dir("src/test")
            .asFile
            .exists()
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(hasTestSources)
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
}
