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
package com.rafaeloliveira.bilheteria.quality.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

private const val BASE = "com.rafaeloliveira.bilheteria"
private const val GUIDE = "docs/harness/guides/architecture.md"
private const val DOMAIN_PURE = "Domain deve ser Kotlin puro. Mova o código para data/ui. Ver $GUIDE (AP-03)"
private const val USE_CASE_SHAPE =
    "UseCase: pacote ..domain.., uma única função pública 'operator fun invoke'. Ver $GUIDE"
private const val VIEW_MODEL_SHAPE =
    "ViewModel: apenas 'val uiState: StateFlow<...>' e 'fun onEvent(event)' públicos; " +
        "MutableStateFlow privado. Ver docs/harness/guides/compose-ui.md (AP-04)"
private const val CIELO_ISOLATED =
    "Deep link lio:// e Base64 da Cielo só no pacote ..data.cielo.. (ADR-0010). Use PaymentGateway fora dele."
private const val SCREEN_STATELESS = "Passe uiState e onEvent para a Screen; só a Route conhece o ViewModel (AP-05)"

/** Sensor S-06. Failure messages are written for the agent: what is wrong + how to fix + guide. */
class ArchitectureTest {
    // docs/ holds reference templates (com.example.*), not production code.
    private val production = Konsist.scopeFromProduction().slice { "/docs/" !in it.path }

    @Test
    fun `camadas respeitam a direcao ui para domain e data para domain`() {
        val packages = production.files.mapNotNull { it.packagee?.name }.filter { it.startsWith(BASE) }
        // Konsist rejects empty layers; the rule starts to apply once a feature has all three.
        val present = listOf("domain", "data", "ui").all { layer -> packages.any { ".$layer" in it } }
        if (!present) return

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
            .assertFalse(additionalMessage = DOMAIN_PURE) {
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
        production
            .classes()
            .withNameEndingWith("UseCase")
            .assertTrue(additionalMessage = USE_CASE_SHAPE) {
                it.resideInPackage("..domain..") &&
                    it.functions().count { fn -> fn.hasPublicOrDefaultModifier } == 1 &&
                    it.hasFunction { fn -> fn.name == "invoke" && fn.hasOperatorModifier }
            }
    }

    @Test
    fun `viewmodel expoe um unico StateFlow uiState e um unico onEvent`() {
        production
            .classes()
            .withNameEndingWith("ViewModel")
            .assertTrue(additionalMessage = VIEW_MODEL_SHAPE) { vm ->
                val publicProps = vm.properties().filter { it.hasPublicOrDefaultModifier }
                val publicFuns = vm.functions().filter { it.hasPublicOrDefaultModifier }
                publicProps.map { it.name } == listOf("uiState") &&
                    publicProps.none { it.type?.name?.startsWith("Mutable") == true } &&
                    publicFuns.map { it.name } == listOf("onEvent")
            }
    }

    @Test
    fun `screens sao stateless e nao recebem ViewModel`() {
        production
            .functions()
            .filter { it.hasAnnotationWithName("Composable") && it.name.endsWith("Screen") }
            .assertFalse(additionalMessage = SCREEN_STATELESS) {
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
    fun `integracao Cielo fica isolada no pacote data cielo`() {
        production.files
            .filter { it.packagee?.name?.startsWith(BASE) == true && ".data.cielo" !in it.packagee?.name.orEmpty() }
            .assertFalse(additionalMessage = CIELO_ISOLATED) {
                "lio://" in it.text || it.hasImport { import -> import.name == "android.util.Base64" }
            }
    }

    @Test
    fun `repositorios implementados no data e interfaces no domain`() {
        production
            .interfaces()
            .withNameEndingWith("Repository")
            .assertTrue(additionalMessage = "Interface de repositório pertence ao domain. Ver $GUIDE") {
                it.resideInPackage("..domain..")
            }
    }
}
