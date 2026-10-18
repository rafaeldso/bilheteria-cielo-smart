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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId

internal class PurchaseFormattingTest {
    private val saoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")

    /** Currency format uses a no-break space after "R$"; compare with a plain one. */
    private fun Money.plain(): String = format().replace(' ', ' ')

    @Test
    fun `dado 10000 centavos quando formatar entao devolve R$ 100,00 em pt-BR`() {
        assertEquals("R$ 100,00", Money(10_000).plain())
    }

    @Test
    fun `dado 125000 centavos quando formatar entao usa ponto no milhar e virgula nos centavos`() {
        assertEquals("R$ 1.250,00", Money(125_000).plain())
    }

    @Test
    fun `dado 5 centavos quando formatar entao devolve R$ 0,05`() {
        assertEquals("R$ 0,05", Money(5).plain())
    }

    @Test
    fun `dado valor redondo quando separar para fala entao so tem reais`() {
        assertEquals(SpokenMoney(reais = 100, centavos = 0), Money(10_000).spoken())
    }

    @Test
    fun `dado valor com centavos quando separar para fala entao devolve reais e centavos`() {
        assertEquals(SpokenMoney(reais = 52, centavos = 50), Money(5_250).spoken())
    }

    @Test
    fun `dado evento as 23h UTC quando formatar no fuso de Sao Paulo entao mostra dia curto e 20 horas`() {
        val startsAt = Instant.parse("2026-11-15T23:00:00Z")

        assertEquals("dom., 15 nov", startsAt.formatEventDay(saoPaulo))
        assertEquals("20:00", startsAt.formatEventTime(saoPaulo))
    }

    @Test
    fun `dado evento perto da meia-noite UTC quando formatar no fuso de Sao Paulo entao o dia segue o fuso`() {
        val startsAt = Instant.parse("2026-12-20T01:30:00Z")

        assertEquals("sáb., 19 dez", startsAt.formatEventDay(saoPaulo))
        assertEquals("22:30", startsAt.formatEventTime(saoPaulo))
    }
}
