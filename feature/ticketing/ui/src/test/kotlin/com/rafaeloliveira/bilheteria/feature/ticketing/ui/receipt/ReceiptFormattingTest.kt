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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class ReceiptFormattingTest {
    // The currency pattern of pt-BR separates "R$" from the number with a no-break space (U+00A0).
    private val nbsp = " "

    @Test
    fun `dado valor em centavos quando formatar entao usa real com virgula decimal`() {
        assertEquals("R$${nbsp}100,00", Money(10_000).formatForReceipt())
        assertEquals("R$${nbsp}0,05", Money(5).formatForReceipt())
        assertEquals("R$${nbsp}60,90", Money(6_090).formatForReceipt())
    }

    @Test
    fun `dado valor acima de mil quando formatar entao usa ponto como separador de milhar`() {
        assertEquals("R$${nbsp}1.234,56", Money(123_456).formatForReceipt())
    }

    @Test
    fun `dado locale padrao do aparelho diferente quando formatar entao continua em pt-BR`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)

            assertEquals("R$${nbsp}100,00", Money(10_000).formatForReceipt())
            assertEquals(
                ReceiptDateTime(date = "17/10/2026", time = "14:32"),
                Instant.parse("2026-10-17T17:32:00Z").toReceiptDateTime(ZoneOffset.ofHours(-3)),
            )
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `dado valor inteiro em reais quando falar entao so reais`() {
        assertEquals(SpokenMoney(reais = 100, centavos = 0), Money(10_000).toSpokenMoney())
        assertFalse(Money(10_000).toSpokenMoney().hasReaisAndCentavos)
        assertFalse(Money(10_000).toSpokenMoney().onlyCentavos)
    }

    @Test
    fun `dado valor com centavos quando falar entao reais e centavos`() {
        val spoken = Money(1_050).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 10, centavos = 50), spoken)
        assertTrue(spoken.hasReaisAndCentavos)
        assertEquals(10, spoken.reaisQuantity)
    }

    @Test
    fun `dado valor abaixo de um real quando falar entao so centavos`() {
        val spoken = Money(50).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 0, centavos = 50), spoken)
        assertTrue(spoken.onlyCentavos)
        assertFalse(spoken.hasReaisAndCentavos)
    }

    @Test
    fun `dado valor enorme quando falar entao a quantidade de plural nao estoura`() {
        assertEquals(Int.MAX_VALUE, Money(Long.MAX_VALUE).toSpokenMoney().reaisQuantity)
    }

    @Test
    fun `dado instante perto da meia-noite quando mostrar entao a data segue o fuso informado`() {
        val instant = Instant.parse("2026-10-18T01:30:00Z")

        assertEquals(ReceiptDateTime("18/10/2026", "01:30"), instant.toReceiptDateTime(ZoneOffset.UTC))
        assertEquals(ReceiptDateTime("17/10/2026", "22:30"), instant.toReceiptDateTime(ZoneOffset.ofHours(-3)))
    }

    @Test
    fun `dado data e hora com um digito quando mostrar entao completa com zero a esquerda`() {
        val instant = Instant.parse("2026-01-05T09:05:00Z")

        assertEquals(ReceiptDateTime("05/01/2026", "09:05"), instant.toReceiptDateTime(ZoneOffset.UTC))
    }
}
