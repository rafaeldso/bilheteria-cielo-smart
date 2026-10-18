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
import org.junit.jupiter.api.Test
import java.util.Locale

/** The only amount format of the ticketing screens (SCR-01 to SCR-04): "R$ 1.250,00", pt-BR, exact cents. */
class MoneyFormatTest {
    // The space between the symbol and the figures is a no-break space, so the symbol never wraps away.
    private val nbsp = "\u00A0"

    @Test
    fun `dado valor em centavos quando formatar entao usa real com virgula decimal`() {
        assertEquals("R$${nbsp}100,00", Money(10_000).formatBrl())
        assertEquals("R$${nbsp}50,00", Money(5_000).formatBrl())
        assertEquals("R$${nbsp}60,90", Money(6_090).formatBrl())
        assertEquals("R$${nbsp}0,05", Money(5).formatBrl())
        assertEquals("R$${nbsp}0,00", Money(0).formatBrl())
    }

    @Test
    fun `dado valor acima de mil quando formatar entao usa ponto como separador de milhar`() {
        assertEquals("R$${nbsp}1.250,00", Money(125_000).formatBrl())
        assertEquals("R$${nbsp}1.250,50", Money(125_050).formatBrl())
        assertEquals("R$${nbsp}1.234,56", Money(123_456).formatBrl())
    }

    @Test
    fun `dado valor formatado quando olhar o separador entao e um espaco sem quebra`() {
        val text = Money(10_000).formatBrl()

        assertEquals("R$", text.take(2))
        assertEquals(nbsp, text[2].toString())
    }

    @Test
    fun `dado locale padrao do aparelho diferente quando formatar entao continua em pt-BR`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)

            assertEquals("R$${nbsp}1.250,50", Money(125_050).formatBrl())
            assertEquals("R$${nbsp}100,00", Money(10_000).formatBrl())
        } finally {
            Locale.setDefault(previous)
        }
    }
}
