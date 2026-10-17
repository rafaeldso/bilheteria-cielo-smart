// Module: quality:architecture-tests (JVM, test source set). Sensor S-06 (Konsist).
// Failure messages are written for the agent: what is wrong + how to fix + guide.
package com.example.app.quality.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

private const val BASE = "com.example.app" // <base.package>
private const val GUIDE = "docs/harness/guides/architecture.md"

class ArchitectureTest {

    private val production = Konsist.scopeFromProduction()

    @Test
    fun `camadas respeitam a direcao ui para domain e data para domain`() {
        production.assertArchitecture {
            val domain = Layer("Domain", "$BASE..domain..")
            val data = Layer("Data", "$BASE..data..")
            val ui = Layer("UI", "$BASE..ui..")

            domain.dependsOnNothing()
            data.dependsOn(domain)
            ui.dependsOn(domain)
        }
    }

    @Test
    fun `domain nao importa Android nem infraestrutura`() {
        production.files
            .filter { it.packagee?.name?.contains(".domain") == true }
            .assertFalse(additionalMessage = "Domain deve ser Kotlin puro. Mova o código para data/ui. Ver $GUIDE (AP-03)") {
                it.hasImport { import ->
                    import.name.startsWith("android.") ||
                        import.name.startsWith("androidx.") ||
                        import.name.startsWith("retrofit2.") ||
                        import.name.startsWith("okhttp3.")
                }
            }
    }

    @Test
    fun `use cases ficam no domain e expoem apenas um operator invoke`() {
        production.classes()
            .withNameEndingWith("UseCase")
            .assertTrue(additionalMessage = "UseCase: pacote ..domain.., uma única função pública 'operator fun invoke'. Ver $GUIDE") {
                it.resideInPackage("..domain..") &&
                    it.functions().count { fn -> fn.hasPublicOrDefaultModifier } == 1 &&
                    it.hasFunction { fn -> fn.name == "invoke" && fn.hasOperatorModifier }
            }
    }

    @Test
    fun `viewmodel expoe um unico StateFlow uiState e um unico onEvent`() {
        production.classes()
            .withNameEndingWith("ViewModel")
            .assertTrue(
                additionalMessage = "ViewModel: apenas 'val uiState: StateFlow<...>' e 'fun onEvent(event)' públicos; " +
                    "MutableStateFlow privado. Ver docs/harness/guides/compose-ui.md (AP-04)",
            ) { vm ->
                val publicProps = vm.properties().filter { it.hasPublicOrDefaultModifier }
                val publicFuns = vm.functions().filter { it.hasPublicOrDefaultModifier }
                publicProps.map { it.name } == listOf("uiState") &&
                    publicProps.none { it.type?.name?.startsWith("Mutable") == true } &&
                    publicFuns.map { it.name } == listOf("onEvent")
            }
    }

    @Test
    fun `screens sao stateless e nao recebem ViewModel`() {
        production.functions()
            .filter { it.hasAnnotationWithName("Composable") && it.name.endsWith("Screen") }
            .assertFalse(additionalMessage = "Passe uiState e onEvent para a Screen; só a Route conhece o ViewModel (AP-05)") {
                it.parameters.any { p -> p.type.name.endsWith("ViewModel") }
            }
    }

    @Test
    fun `DTOs nao saem da camada data`() {
        production.files
            .filter { it.packagee?.name?.let { pkg -> ".domain" in pkg || ".ui" in pkg } == true }
            .assertFalse(additionalMessage = "Mapeie DTO para modelo de domínio no data (toDomain()). AP-06") {
                it.hasImport { import -> import.name.endsWith("Dto") }
            }
    }

    @Test
    fun `repositorios implementados no data e interfaces no domain`() {
        production.interfaces()
            .withNameEndingWith("Repository")
            .assertTrue(additionalMessage = "Interface de repositório pertence ao domain. Ver $GUIDE") {
                it.resideInPackage("..domain..")
            }
    }
}
