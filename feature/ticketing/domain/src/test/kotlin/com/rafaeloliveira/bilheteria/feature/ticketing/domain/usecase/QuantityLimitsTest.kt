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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuantityLimitsTest {
    @Test
    fun `AC-04 - dado evento de 50 reais quando selecionar entao quantidade 1 e total 50`() {
        val event = anEvent(unitPriceCents = 5_000)

        val quantity = QuantityLimits.MIN

        assertEquals(1, quantity)
        assertEquals(Money(5_000), event.unitPrice * quantity)
    }

    @Test
    fun `AC-05 - dado quantidade 1 quando aumentar para 3 entao total 150`() {
        val event = anEvent(unitPriceCents = 5_000)
        var quantity = QuantityLimits.MIN

        repeat(2) {
            assertTrue(QuantityLimits.canIncrement(quantity, available = 50))
            quantity++
        }

        assertEquals(3, quantity)
        assertEquals(Money(15_000), event.unitPrice * quantity)
    }

    @Test
    fun `AC-06 - dado limite de 10 por compra quando estiver no valor entao aumentar fica indisponivel`() {
        assertTrue(QuantityLimits.canIncrement(quantity = 9, available = 50))
        assertFalse(QuantityLimits.canIncrement(quantity = 10, available = 50))
        assertFalse(QuantityLimits.canIncrement(quantity = 11, available = 50))
    }

    @Test
    fun `AC-06 - dado menos de 10 disponiveis quando chegar ao disponivel entao aumentar fica indisponivel`() {
        assertTrue(QuantityLimits.canIncrement(quantity = 2, available = 3))
        assertFalse(QuantityLimits.canIncrement(quantity = 3, available = 3))
        assertFalse(QuantityLimits.canIncrement(quantity = 4, available = 3))
    }

    @Test
    fun `AC-06 - dado um unico ingresso disponivel quando quantidade for 1 entao aumentar fica indisponivel`() {
        assertFalse(QuantityLimits.canIncrement(quantity = 1, available = 1))
        assertFalse(QuantityLimits.canIncrement(quantity = 1, available = 0))
    }

    @Test
    fun `AC-06 - dado quantidade minima quando diminuir entao fica indisponivel so no minimo`() {
        assertFalse(QuantityLimits.canDecrement(quantity = 0))
        assertFalse(QuantityLimits.canDecrement(quantity = 1))
        assertTrue(QuantityLimits.canDecrement(quantity = 2))
    }

    @Test
    fun `AC-06 - dado disponiveis ao redor de 10 quando calcular maximo entao nunca passa de 10`() {
        assertEquals(0, QuantityLimits.max(available = 0))
        assertEquals(3, QuantityLimits.max(available = 3))
        assertEquals(9, QuantityLimits.max(available = 9))
        assertEquals(10, QuantityLimits.max(available = 10))
        assertEquals(10, QuantityLimits.max(available = 11))
        assertEquals(10, QuantityLimits.max(available = 50))
    }

    @Test
    fun `AC-06 - dado constantes de BR-05 e BR-07 quando consultar entao minimo 1 e maximo 10`() {
        assertEquals(1, QuantityLimits.MIN)
        assertEquals(10, QuantityLimits.MAX_PER_PURCHASE)
    }
}
