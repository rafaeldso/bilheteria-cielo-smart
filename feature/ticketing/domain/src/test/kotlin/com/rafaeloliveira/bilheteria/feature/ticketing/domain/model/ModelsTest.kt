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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.model

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ModelsTest {
    @Test
    fun `money multiplica por quantidade sem perder centavos`() {
        assertEquals(Money(15_000), Money(5_000) * 3)
        assertEquals(Money(1_050), Money(350) * 3)
    }

    @Test
    fun `money nao aceita valor negativo`() {
        assertThrows<IllegalArgumentException> { Money(-1) }
    }

    @Test
    fun `referencia da compra nao pode ser vazia`() {
        assertThrows<IllegalArgumentException> { PurchaseReference(" ") }
        assertEquals("abc", PurchaseReference("abc").value)
    }

    @ParameterizedTest
    @EnumSource(PurchaseStatus::class)
    fun `so pending e recovering sao compras abertas e so resultados de venda vao ao historico`(
        status: PurchaseStatus,
    ) {
        val open = status == PurchaseStatus.Pending || status == PurchaseStatus.Recovering
        val sale =
            status == PurchaseStatus.Approved || status == PurchaseStatus.Declined || status == PurchaseStatus.Cancelled
        assertEquals(open, status.isOpen)
        assertEquals(sale, status.isSale)
    }

    @Test
    fun `meses ordenam por ano e depois por mes`() {
        val months = listOf(EventMonth(2027, 1), EventMonth(2026, 12), EventMonth(2026, 11))
        assertEquals(listOf(EventMonth(2026, 11), EventMonth(2026, 12), EventMonth(2027, 1)), months.sorted())
    }

    @Test
    fun `compra guarda snapshot do evento e total coerente`() {
        val event = anEvent(unitPriceCents = 5_000)
        val purchase = aPurchase(event = event, quantity = 2, transaction = aTransaction())
        assertEquals(event.name, purchase.eventName)
        assertEquals(Money(10_000), purchase.total)
        assertEquals("A1B2C3", purchase.transaction?.authCode)
        assertEquals(48, AvailableEvent(event, available = 48).available)
    }

    @Test
    fun `resultados de pagamento expõem a referencia so quando a Cielo a devolve`() {
        val reference = PurchaseReference("ref-1")
        val approved = PaymentOutcome.Approved(reference, Money(10_000), aTransaction())
        val page =
            PaymentOutcome.LookupPage(
                page = 1,
                totalPages = 3,
                orders = listOf(LookupOrder(reference, approved)),
            )
        assertEquals(reference, approved.reference)
        assertEquals(reference, PaymentOutcome.Declined(reference).reference)
        assertEquals(reference, PaymentOutcome.Cancelled(reference).reference)
        assertEquals(FailureReason.Generic, PaymentOutcome.Failed(reference, FailureReason.Generic).reason)
        assertNull(PaymentOutcome.Unreadable.reference)
        assertNull(page.reference)
        assertTrue(page.orders.single().approved != null)
        assertFalse(LookupOrder(reference, approved = null).approved != null)
    }
}
