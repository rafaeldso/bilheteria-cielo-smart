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

import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.PtBr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", PtBr)
private val DayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", PtBr)

/**
 * When a purchase was made, relative to "now" (DESIGN.md section 9): today, yesterday, then `dd/MM`.
 * Only values here; the words come from `strings_history.xml`.
 */
internal sealed interface HistoryMoment {
    /** Local time as `HH:mm`. */
    val time: String

    data class Today(
        override val time: String,
    ) : HistoryMoment

    data class Yesterday(
        override val time: String,
    ) : HistoryMoment

    /** [shortDate] is `dd/MM`; [day] and [monthName] ("outubro") feed the spoken form. */
    data class OtherDay(
        val shortDate: String,
        val day: Int,
        val monthName: String,
        override val time: String,
    ) : HistoryMoment
}

internal fun historyMoment(
    createdAt: Instant,
    now: Instant,
    zone: ZoneId,
): HistoryMoment {
    val purchase = createdAt.atZone(zone)
    val date: LocalDate = purchase.toLocalDate()
    val today: LocalDate = now.atZone(zone).toLocalDate()
    val time = TimeFormatter.format(purchase)
    return when (date) {
        today -> {
            HistoryMoment.Today(time)
        }

        today.minusDays(1) -> {
            HistoryMoment.Yesterday(time)
        }

        else -> {
            HistoryMoment.OtherDay(
                shortDate = DayMonthFormatter.format(purchase),
                day = date.dayOfMonth,
                monthName = date.month.getDisplayName(TextStyle.FULL, PtBr),
                time = time,
            )
        }
    }
}
