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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Event day and time and the spoken date of an instant: the zone is always explicit, the locale always pt-BR. */
class EventDatePartsTest {
    private val saoPaulo: ZoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `dado evento as 23h UTC quando formatar no fuso de Sao Paulo entao mostra dia curto e 20 horas`() {
        val startsAt = Instant.parse("2026-11-15T23:00:00Z")

        assertEquals(EventDateParts("dom.", 15, "nov", 20, 0), startsAt.shortEventDate(saoPaulo))
        assertEquals("20:00", startsAt.shortEventDate(saoPaulo).time)
    }

    @Test
    fun `dado evento perto da meia-noite UTC quando formatar no fuso de Sao Paulo entao o dia segue o fuso`() {
        val startsAt = Instant.parse("2026-12-20T01:30:00Z")

        assertEquals(EventDateParts("sáb.", 19, "dez", 22, 30), startsAt.shortEventDate(saoPaulo))
        assertEquals(EventDateParts("sábado", 19, "dezembro", 22, 30), startsAt.fullEventDate(saoPaulo))
        assertEquals("22:30", startsAt.shortEventDate(saoPaulo).time)
    }

    @Test
    fun `dado hora e minuto de um digito quando mostrar a hora entao completa com zero a esquerda`() {
        assertEquals("09:05", Instant.parse("2026-11-14T12:05:00Z").shortEventDate(saoPaulo).time)
    }

    @Test
    fun `dado locale padrao do aparelho diferente quando formatar a data entao continua em pt-BR`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)

            assertEquals(
                EventDateParts("domingo", 15, "novembro", 20, 0),
                Instant.parse("2026-11-15T23:00:00Z").fullEventDate(saoPaulo),
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `dado instante e fuso quando falar a data entao devolve dia mes por extenso e hora`() {
        val spoken = Instant.parse("2026-10-17T17:32:00Z").toSpokenDateTime(saoPaulo)

        assertEquals(SpokenDateTime(day = 17, monthName = "outubro", time = "14:32"), spoken)
    }

    @Test
    fun `dado instante perto da meia-noite quando falar a data entao o dia segue o fuso informado`() {
        val instant = Instant.parse("2026-10-18T01:30:00Z")

        assertEquals(SpokenDateTime(18, "outubro", "01:30"), instant.toSpokenDateTime(ZoneId.of("UTC")))
        assertEquals(SpokenDateTime(17, "outubro", "22:30"), instant.toSpokenDateTime(saoPaulo))
    }

    @Test
    fun `dado dia e hora de um digito quando falar a data entao o dia nao tem zero e a hora tem`() {
        val spoken = Instant.parse("2026-03-05T12:05:00Z").toSpokenDateTime(saoPaulo)

        assertEquals(SpokenDateTime(day = 5, monthName = "março", time = "09:05"), spoken)
    }
}
