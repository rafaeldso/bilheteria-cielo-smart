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

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalResources
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle

private const val ABBREVIATION_LENGTH = 3
private const val TIME_DIGITS = 2

/**
 * Day and time of an event in the display zone. [weekday] and [month] are abbreviated ("sáb.", "nov") or in full
 * ("sábado", "novembro") depending on the factory that built it ([shortEventDate], [fullEventDate]).
 */
@Immutable
internal data class EventDateParts(
    val weekday: String,
    val day: Int,
    val month: String,
    val hour: Int,
    val minute: Int,
) {
    /** 24-hour clock, "20:00". */
    val time: String get() = "${hour.twoDigits()}:${minute.twoDigits()}"
}

private fun Int.twoDigits(): String = toString().padStart(TIME_DIGITS, '0')

/** Abbreviated day for a line of text: "sáb.", 15, "nov", 20:00. */
internal fun Instant.shortEventDate(zone: ZoneId): EventDateParts {
    val parts = fullEventDate(zone)
    return parts.copy(
        weekday = parts.weekday.take(ABBREVIATION_LENGTH) + ".",
        month = parts.month.take(ABBREVIATION_LENGTH),
    )
}

/** Day in full for TalkBack: "sábado", 15, "novembro", 20:00. The zone is explicit, never the device default. */
internal fun Instant.fullEventDate(zone: ZoneId): EventDateParts {
    val local = atZone(zone)
    return EventDateParts(
        weekday = weekdayName(local.dayOfWeek),
        day = local.dayOfMonth,
        month = monthName(local.monthValue),
        hour = local.hour,
        minute = local.minute,
    )
}

/** Month in full, lower case, pt-BR ("novembro"). */
internal fun monthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL, PtBr).lowercase(PtBr)

private fun weekdayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, PtBr).lowercase(PtBr)

/** "sábado, 15 de novembro, às 20 horas": the day in full and the time as a person would say it. */
@Composable
internal fun spokenEventDate(parts: EventDateParts): String = LocalResources.current.spokenEventDateText(parts)

/** The resource lookup behind [spokenEventDate]; usable outside composition, e.g. inside a `remember`. */
internal fun Resources.spokenEventDateText(parts: EventDateParts): String =
    getString(R.string.ticketing_common_event_date_spoken, parts.weekday, parts.day, parts.month, spokenTime(parts))

private fun Resources.spokenTime(parts: EventDateParts): String =
    when {
        parts.hour == 0 && parts.minute == 0 -> {
            getString(R.string.ticketing_common_spoken_midnight)
        }

        parts.hour == 0 -> {
            getString(R.string.ticketing_common_spoken_midnight_minutes, parts.minute)
        }

        parts.minute == 0 -> {
            getQuantityString(R.plurals.ticketing_common_spoken_hour, parts.hour, parts.hour)
        }

        else -> {
            getQuantityString(R.plurals.ticketing_common_spoken_hour_minutes, parts.hour, parts.hour, parts.minute)
        }
    }
