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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.eventlist

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.EventDateParts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/** Pure formats of SCR-01: pt-BR names must not depend on the device default locale. Amounts: `MoneyFormatTest`. */
class EventListFormatsTest {
    private val utc = EventListFormats(zoneId = ZoneOffset.UTC, currentYear = 2026)

    // 2026-11-14 is a Saturday.
    private val saturdayEvening = Instant.parse("2026-11-14T20:00:00Z")

    @Test
    fun `data curta usa dia da semana e mes abreviados em pt-BR`() {
        assertEquals(EventDateParts("sáb.", 14, "nov", 20, 0), utc.shortDate(saturdayEvening))
        assertEquals("20:00", utc.shortDate(saturdayEvening).time)
    }

    @Test
    fun `data por extenso usa dia da semana e mes completos em pt-BR`() {
        assertEquals(EventDateParts("sábado", 14, "novembro", 20, 0), utc.fullDate(saturdayEvening))
    }

    @Test
    fun `abreviacao de todos os meses e dias da semana segue o padrao pt-BR`() {
        val months =
            (1..12).map {
                utc
                    .shortDate(
                        Instant.parse("2026-%02d-10T12:00:00Z".format(Locale.ROOT, it)),
                    ).month
            }
        assertEquals(listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"), months)
        val weekdays =
            (9..15).map {
                utc
                    .shortDate(
                        Instant.parse("2026-11-%02dT12:00:00Z".format(Locale.ROOT, it)),
                    ).weekday
            }
        assertEquals(listOf("seg.", "ter.", "qua.", "qui.", "sex.", "sáb.", "dom."), weekdays)
    }

    @Test
    fun `hora e minuto com um digito ganham zero a esquerda`() {
        assertEquals("09:05", utc.shortDate(Instant.parse("2026-11-14T09:05:00Z")).time)
    }

    @Test
    fun `horario do evento e mostrado no fuso de exibicao e nao em UTC`() {
        val saoPaulo = EventListFormats(zoneId = ZoneId.of("America/Sao_Paulo"), currentYear = 2026)

        // 01:30 UTC of December 1st is 22:30 of November 30th in São Paulo (UTC-3).
        val parts = saoPaulo.fullDate(Instant.parse("2026-12-01T01:30:00Z"))

        assertEquals(EventDateParts("segunda-feira", 30, "novembro", 22, 30), parts)
    }

    @Test
    fun `chip do mes no ano corrente mostra so o mes abreviado com inicial maiuscula`() {
        assertEquals(MonthLabel(name = "Nov", year = null), utc.monthChip(EventMonth(2026, 11)))
        assertEquals(MonthLabel(name = "Mar", year = null), utc.monthChip(EventMonth(2026, 3)))
    }

    @Test
    fun `chip do mes em outro ano carrega o ano`() {
        assertEquals(MonthLabel(name = "Jan", year = 2027), utc.monthChip(EventMonth(2027, 1)))
    }

    @Test
    fun `cabecalho do mes mostra o mes por extenso e sempre o ano`() {
        assertEquals(MonthLabel(name = "novembro", year = 2026), utc.monthHeader(EventMonth(2026, 11)))
        assertEquals(MonthLabel(name = "janeiro", year = 2027), utc.monthHeader(EventMonth(2027, 1)))
    }

    @Test
    fun `formatos independem do locale padrao do aparelho`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("sábado", utc.fullDate(saturdayEvening).weekday)
        } finally {
            Locale.setDefault(previous)
        }
    }
}
