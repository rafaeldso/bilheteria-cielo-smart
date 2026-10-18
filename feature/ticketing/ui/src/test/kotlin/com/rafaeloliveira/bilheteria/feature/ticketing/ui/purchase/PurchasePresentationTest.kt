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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class PurchasePresentationTest {
    private val event =
        EventSummary(
            id = "e1",
            name = "Noite de Jazz no Parque",
            startsAt = Instant.parse("2026-11-15T23:00:00Z"),
            venue = "Concha Acústica Municipal",
            unitPrice = Money(5_000),
            available = null,
        )
    private val total = Money(10_000)
    private val awaiting = PurchaseUiState.AwaitingPayment(event, quantity = 2, total = total)
    private val recovering = PurchaseUiState.Recovering(event, quantity = 2, total = total)
    private val error = PurchaseUiState.Error(event, quantity = 2, total = total, reason = PurchaseErrorReason.Generic)
    private val editing =
        PurchaseUiState.Editing(
            event = event,
            quantity = 2,
            canDecrement = true,
            canIncrement = true,
            limitHint = null,
            total = total,
        )

    @Test
    fun `dado pagamento aberto ou em confirmacao quando voltar do sistema entao e ignorado`() {
        assertTrue(awaiting.blocksSystemBack())
        assertTrue(recovering.blocksSystemBack())
    }

    @Test
    fun `dado estados sem pagamento aberto quando voltar do sistema entao nao e bloqueado`() {
        listOf(PurchaseUiState.Loading, editing, error, PurchaseUiState.Completed("p1")).forEach {
            assertFalse(it.blocksSystemBack(), "$it")
        }
    }

    @Test
    fun `dado pagamento aberto ou em confirmacao quando montar a barra entao nao oferece a seta de voltar`() {
        assertFalse(awaiting.showsNavigateUp())
        assertFalse(recovering.showsNavigateUp())
    }

    @Test
    fun `dado edicao ou erro quando montar a barra entao oferece a seta de voltar`() {
        listOf(PurchaseUiState.Loading, editing, error).forEach { assertTrue(it.showsNavigateUp(), "$it") }
    }

    @Test
    fun `AC-26 - dado evento indisponivel quando abrir a compra entao a saida para os eventos nao fica bloqueada`() {
        assertTrue(PurchaseUiState.Unavailable.showsNavigateUp())
        assertFalse(PurchaseUiState.Unavailable.blocksSystemBack())
    }

    @Test
    fun `AC-25 - dado consulta que falhou quando mostrar o erro entao o botao consulta de novo em vez de cobrar`() {
        assertEquals(RetryAction.CheckAgain, PurchaseErrorReason.RecoveryFailed.retryAction())
    }

    @Test
    fun `dado erro de pagamento quando mostrar o erro entao o botao tenta pagar de novo`() {
        val reasons = PurchaseErrorReason.entries - PurchaseErrorReason.RecoveryFailed

        reasons.forEach { assertEquals(RetryAction.PayAgain, it.retryAction(), "$it") }
    }

    @Test
    fun `dado cada variante de erro quando mostrar a mensagem entao titulo e texto sao proprios`() {
        val reasons = PurchaseErrorReason.entries

        assertEquals(reasons.size, reasons.map { it.titleRes() }.toSet().size)
        assertEquals(reasons.size, reasons.map { it.bodyRes() }.toSet().size)
    }

    @Test
    fun `AC-06 - dado limite de 10 por compra quando mostrar a dica entao usa o texto de maximo com o limite`() {
        val text = LimitHint.MaxPerPurchase(max = 10).text()

        assertEquals(LimitHintText.Fixed(R.string.ticketing_purchase_limit_max, 10), text)
    }

    @Test
    fun `AC-06 - dado so 3 ingressos disponiveis quando mostrar a dica entao usa o plural de restantes`() {
        val text = LimitHint.OnlyAvailable(available = 3).text()

        assertEquals(LimitHintText.Counted(R.plurals.ticketing_purchase_limit_available, 3), text)
    }
}
