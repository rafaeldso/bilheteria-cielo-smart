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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** Absolute dates of SCR-03. Amounts are shared: `MoneyFormatTest`, `SpokenMoneyTest`. */
class ReceiptFormattingTest {
    @Test
    fun `dado locale padrao do aparelho diferente quando mostrar a data entao continua em pt-BR`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)

            assertEquals(
                ReceiptDateTime(date = "17/10/2026", time = "14:32"),
                Instant.parse("2026-10-17T17:32:00Z").toReceiptDateTime(ZoneOffset.ofHours(-3)),
            )
        } finally {
            Locale.setDefault(original)
        }
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
