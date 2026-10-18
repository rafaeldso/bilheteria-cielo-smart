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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.format

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** How an amount is split to be read aloud; the words come from `strings_common.xml`. */
class SpokenMoneyTest {
    @Test
    fun `dado valor inteiro em reais quando falar entao so reais`() {
        val spoken = Money(10_000).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 100, centavos = 0), spoken)
        assertFalse(spoken.hasReaisAndCentavos)
        assertFalse(spoken.onlyCentavos)
        assertFalse(spoken.isZero)
    }

    @Test
    fun `dado valor com centavos quando falar entao reais e centavos`() {
        val spoken = Money(1_050).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 10, centavos = 50), spoken)
        assertTrue(spoken.hasReaisAndCentavos)
        assertEquals(10, spoken.reaisQuantity)
        assertEquals(SpokenMoney(reais = 52, centavos = 50), Money(5_250).toSpokenMoney())
        assertEquals(SpokenMoney(reais = 50, centavos = 90), Money(5_090).toSpokenMoney())
    }

    @Test
    fun `dado valor abaixo de um real quando falar entao so centavos`() {
        val spoken = Money(50).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 0, centavos = 50), spoken)
        assertTrue(spoken.onlyCentavos)
        assertFalse(spoken.hasReaisAndCentavos)
        assertFalse(spoken.isZero)
        assertEquals(SpokenMoney(reais = 0, centavos = 5), Money(5).toSpokenMoney())
    }

    @Test
    fun `dado valor zero quando falar entao e zero reais e nao um plural`() {
        val spoken = Money(0).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 0, centavos = 0), spoken)
        assertTrue(spoken.isZero)
        assertFalse(spoken.onlyCentavos)
        assertFalse(spoken.hasReaisAndCentavos)
    }

    @Test
    fun `dado valor acima de mil quando falar entao os reais ficam inteiros sem separador`() {
        assertEquals(SpokenMoney(reais = 1_250, centavos = 0), Money(125_000).toSpokenMoney())
        assertEquals(SpokenMoney(reais = 1_250, centavos = 50), Money(125_050).toSpokenMoney())
    }

    @Test
    fun `dado valor enorme quando falar entao a quantidade de plural nao estoura`() {
        assertEquals(Int.MAX_VALUE, Money(Long.MAX_VALUE).toSpokenMoney().reaisQuantity)
    }
}
