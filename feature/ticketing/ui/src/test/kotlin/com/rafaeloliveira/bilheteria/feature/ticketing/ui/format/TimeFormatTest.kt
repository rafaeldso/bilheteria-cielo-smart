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

/** The single time-of-day format of the ticketing screens: 24-hour clock with leading zeros, in the given zone. */
class TimeFormatTest {
    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `dado instante em UTC quando formatar no fuso de Sao Paulo entao usa hora local com zero a esquerda`() {
        val instant = Instant.parse("2026-10-17T12:05:00Z")

        assertEquals("09:05", formatTime24(instant.atZone(saoPaulo)))
    }

    @Test
    fun `dado locale padrao diferente quando formatar entao mantem digitos latinos de 24 horas`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-SA"))

            assertEquals("21:30", formatTime24(Instant.parse("2026-10-17T00:30:00Z").atZone(saoPaulo)))
        } finally {
            Locale.setDefault(original)
        }
    }
}
