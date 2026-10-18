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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.history

import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusBadgeVariant
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Pure formatting of SCR-04 (dates, amounts and status). Row rendering itself is out of scope for JVM tests. */
class HistoryFormattingTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    // 17/10/2026 17:00 in Sao Paulo (UTC-3).
    private val now = Instant.parse("2026-10-17T20:00:00Z")

    @Test
    fun `formats money in pt-BR with the currency symbol and a non-breaking space`() {
        assertEquals("R$ 100,00", formatMoney(Money(10_000)))
        assertEquals("R$ 1.250,50", formatMoney(Money(125_050)))
        assertEquals("R$ 0,05", formatMoney(Money(5)))
        assertEquals("R$ 0,00", formatMoney(Money(0)))
    }

    @Test
    fun `formats money in pt-BR even when the device locale is another`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("R$ 1.250,50", formatMoney(Money(125_050)))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `spells out whole reais`() {
        val spoken = Money(10_000).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 100, cents = 0), spoken)
        assertTrue(spoken.hasReais)
        assertFalse(spoken.hasCents)
    }

    @Test
    fun `spells out reais and cents together`() {
        val spoken = Money(10_550).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 105, cents = 50), spoken)
        assertTrue(spoken.hasReais)
        assertTrue(spoken.hasCents)
    }

    @Test
    fun `spells out an amount below one real as cents only`() {
        val spoken = Money(5).toSpokenMoney()

        assertEquals(SpokenMoney(reais = 0, cents = 5), spoken)
        assertFalse(spoken.hasReais)
        assertTrue(spoken.hasCents)
    }

    @Test
    fun `spells out a zero amount as zero reais`() {
        val spoken = Money(0).toSpokenMoney()

        assertTrue(spoken.hasReais)
        assertFalse(spoken.hasCents)
    }

    @Test
    fun `a purchase made earlier on the same local day is today`() {
        val moment = historyMoment(Instant.parse("2026-10-17T17:32:00Z"), now, zone)

        assertEquals(HistoryMoment.Today(time = "14:32"), moment)
    }

    @Test
    fun `a purchase made on the previous local day is yesterday`() {
        val moment = historyMoment(Instant.parse("2026-10-16T21:10:00Z"), now, zone)

        assertEquals(HistoryMoment.Yesterday(time = "18:10"), moment)
    }

    @Test
    fun `the day is decided by the local zone and not by UTC`() {
        // 02:30 UTC on the 17th is 23:30 on the 16th in Sao Paulo: yesterday, even though "now" is the 17th in UTC.
        val moment = historyMoment(Instant.parse("2026-10-17T02:30:00Z"), now, zone)

        assertEquals(HistoryMoment.Yesterday(time = "23:30"), moment)
    }

    @Test
    fun `an older purchase shows day and month with the month name for speech`() {
        val moment = historyMoment(Instant.parse("2026-10-15T12:00:00Z"), now, zone)

        assertEquals(
            HistoryMoment.OtherDay(shortDate = "15/10", day = 15, monthName = "outubro", time = "09:00"),
            moment,
        )
    }

    @Test
    fun `a leading zero is kept in the day and month`() {
        val moment = historyMoment(Instant.parse("2026-03-05T15:05:00Z"), now, zone) as HistoryMoment.OtherDay

        assertEquals("05/03", moment.shortDate)
        assertEquals(5, moment.day)
        assertEquals("março", moment.monthName)
        assertEquals("12:05", moment.time)
    }

    @Test
    fun `only approved declined and cancelled purchases have a history status`() {
        assertEquals(HistoryStatus.Approved, PurchaseStatus.Approved.toHistoryStatus())
        assertEquals(HistoryStatus.Declined, PurchaseStatus.Declined.toHistoryStatus())
        assertEquals(HistoryStatus.Cancelled, PurchaseStatus.Cancelled.toHistoryStatus())
        assertNull(PurchaseStatus.Pending.toHistoryStatus())
        assertNull(PurchaseStatus.Recovering.toHistoryStatus())
        assertNull(PurchaseStatus.Failed.toHistoryStatus())
    }

    @Test
    fun `every status the history shows maps to a distinct badge variant`() {
        assertEquals(DsStatusBadgeVariant.Success, HistoryStatus.Approved.badgeVariant())
        assertEquals(DsStatusBadgeVariant.Error, HistoryStatus.Declined.badgeVariant())
        assertEquals(DsStatusBadgeVariant.Neutral, HistoryStatus.Cancelled.badgeVariant())
    }
}
