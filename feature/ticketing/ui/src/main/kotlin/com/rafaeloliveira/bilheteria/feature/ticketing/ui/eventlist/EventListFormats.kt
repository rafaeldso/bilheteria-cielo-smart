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

import androidx.compose.runtime.Immutable
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.EventDateParts
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.PtBr
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.fullEventDate
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.monthName
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.shortEventDate
import java.time.Instant
import java.time.ZoneId

private const val ABBREVIATION_LENGTH = 3

/** A month for a chip or a header: [name] in pt-BR and the [year] when it must be shown (null when it must not). */
internal data class MonthLabel(
    val name: String,
    val year: Int?,
)

/**
 * What is specific to SCR-01: month chips and headers, and the event date in the display zone. Amounts and the
 * spoken texts are shared with the other screens (`ui.format`). Everything uses an explicit pt-BR locale (never the
 * device default) and `java.time`, so the result is the same on the terminal, on a phone and in the JVM tests.
 *
 * @property zoneId zone to show event times in; it must be the zone the domain uses to group by month (BR-22).
 * @property currentYear year of the clock; chips of other years show the year (ui-spec 4.4).
 */
@Immutable
internal data class EventListFormats(
    val zoneId: ZoneId,
    val currentYear: Int,
) {
    /** Chip label: "Nov", or "Jan" with year 2027 when it is not the current year. */
    fun monthChip(month: EventMonth): MonthLabel =
        MonthLabel(
            name = monthName(month.month).take(ABBREVIATION_LENGTH).replaceFirstChar { it.titlecase(PtBr) },
            year = month.year.takeIf { it != currentYear },
        )

    /** Header and spoken chip label: the month in full and always its year ("novembro de 2026"). */
    fun monthHeader(month: EventMonth): MonthLabel = MonthLabel(name = monthName(month.month), year = month.year)

    /** Abbreviated day for the item line: "sáb.", 15, "nov", 20:00. */
    fun shortDate(startsAt: Instant): EventDateParts = startsAt.shortEventDate(zoneId)

    /** Day in full for TalkBack: "sábado", 15, "novembro", 20:00. */
    fun fullDate(startsAt: Instant): EventDateParts = startsAt.fullEventDate(zoneId)
}
