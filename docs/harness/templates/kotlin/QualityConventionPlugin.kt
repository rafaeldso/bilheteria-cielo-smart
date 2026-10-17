// Module: build-logic/convention. Applied once on the root project (id: app.quality).
// Defines the stable task contract used by scripts/harness/verify.sh:
//   harnessFast  -> spotlessCheck, detekt, architecture tests
//   harnessCheck -> harnessFast + unit tests + Android Lint
//   harnessFull  -> harnessCheck + Roborazzi + Kover + module graph + release build
// Each sensor plugin wires itself lazily, so modules only need their usual plugins.
import org.gradle.api.Plugin
import org.gradle.api.Project

class QualityConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        require(target == target.rootProject) { "app.quality must be applied to the root project" }
        with(target) {
            val fast = tasks.register("harnessFast") { group = HARNESS; description = "Formatação, detekt, arquitetura" }
            val check = tasks.register("harnessCheck") { group = HARNESS; dependsOn(fast) }
            val full = tasks.register("harnessFull") { group = HARNESS; dependsOn(check) }

            pluginManager.withPlugin("com.diffplug.spotless") { fast.configure { dependsOn("spotlessCheck") } }
            pluginManager.withPlugin("org.jetbrains.kotlinx.kover") { full.configure { dependsOn("koverVerify") } }
            pluginManager.withPlugin("com.jraska.module.graph.assertion") { full.configure { dependsOn("assertModuleGraph") } }

            subprojects {
                val module = this
                pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
                    fast.configure { dependsOn(module.tasks.named("detekt")) }
                }
                pluginManager.withPlugin("com.android.library") {
                    check.configure { dependsOn(module.tasks.named("testDebugUnitTest"), module.tasks.named("lintDebug")) }
                }
                pluginManager.withPlugin("com.android.application") {
                    check.configure { dependsOn(module.tasks.named("testDebugUnitTest"), module.tasks.named("lintDebug")) }
                    full.configure { dependsOn(module.tasks.named("assembleRelease")) }
                }
                pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    val target = if (module.path == ARCHITECTURE_TESTS) fast else check
                    target.configure { dependsOn(module.tasks.named("test")) }
                }
                pluginManager.withPlugin("io.github.takahirom.roborazzi") {
                    full.configure { dependsOn(module.tasks.named("verifyRoborazziDebug")) }
                }
            }
        }
    }

    private companion object {
        const val HARNESS = "harness"
        const val ARCHITECTURE_TESTS = ":quality:architecture-tests"
    }
}
