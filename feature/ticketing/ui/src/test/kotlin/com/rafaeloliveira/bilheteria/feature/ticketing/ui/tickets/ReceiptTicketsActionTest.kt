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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.tickets

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptData
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptUiState
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.offersTickets
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class ReceiptTicketsActionTest {
    private val receipt =
        ReceiptData(
            purchaseNumber = "ref",
            eventName = "Noite de Jazz no Parque",
            eventStartsAt = Instant.parse("2026-11-15T23:00:00Z"),
            venue = "Concha Acústica Municipal",
            quantity = 2,
            unitPrice = Money(5_000),
            total = Money(10_000),
            purchasedAt = Instant.parse("2026-10-17T14:32:00Z"),
            transaction = null,
        )

    @Test
    fun `AC-33 - dado comprovante aprovado quando decidir as acoes entao oferece ver ingressos`() {
        assertTrue(ReceiptUiState.Approved(receipt).offersTickets())
    }

    @Test
    fun `AC-33 - dado comprovante negado cancelado em carga ou com erro quando decidir as acoes entao nao oferece`() {
        listOf(
            ReceiptUiState.Declined(receipt),
            ReceiptUiState.Cancelled(receipt),
            ReceiptUiState.Loading,
            ReceiptUiState.Error,
        ).forEach { assertFalse(it.offersTickets(), "$it") }
    }
}
