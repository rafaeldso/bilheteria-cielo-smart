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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private const val ABBREVIATION_LENGTH = 3
private const val CENTS_PER_REAL = 100
private const val CURRENCY_SYMBOL = "R$"
private const val NO_BREAK_SPACE = ' '
private const val PRICE_PATTERN = "#,##0.00"
private const val CENTS_SCALE = 2

/** A month for a chip or a header: [name] in pt-BR and the [year] when it must be shown (null when it must not). */
internal data class MonthLabel(
    val name: String,
    val year: Int?,
)

/**
 * Day and time of an event in the display zone. [weekday] and [month] are abbreviated ("sáb.", "nov") or in full
 * ("sábado", "novembro") depending on the factory that built it.
 */
internal data class EventDateParts(
    val weekday: String,
    val day: Int,
    val month: String,
    val hour: Int,
    val minute: Int,
) {
    /** 24-hour clock, "20:00". */
    val time: String get() = "${hour.toString().padStart(
        TIME_DIGITS,
        '0',
    )}:${minute.toString().padStart(TIME_DIGITS, '0')}"

    private companion object {
        const val TIME_DIGITS = 2
    }
}

/** A price split to be read aloud ("50 reais e 90 centavos"): [reaisText] is grouped ("1.250"). */
internal data class SpokenAmount(
    val reais: Long,
    val reaisText: String,
    val centavos: Int,
)

/**
 * pt-BR formats of SCR-01. Everything uses an explicit `Locale` (never the device default) and `java.time`, so the
 * result is the same on the terminal, on a phone and in the JVM tests. The words around the values ("às", "de",
 * "reais") live in `strings_eventlist.xml`; this class only yields the values and the names of days and months.
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
            name = monthName(month.month).take(ABBREVIATION_LENGTH).replaceFirstChar { it.titlecase(PT_BR) },
            year = month.year.takeIf { it != currentYear },
        )

    /** Header and spoken chip label: the month in full and always its year ("novembro de 2026"). */
    fun monthHeader(month: EventMonth): MonthLabel = MonthLabel(name = monthName(month.month), year = month.year)

    /** Abbreviated day for the item line: "sáb.", 15, "nov", 20:00. */
    fun shortDate(startsAt: Instant): EventDateParts {
        val parts = fullDate(startsAt)
        return parts.copy(
            weekday = parts.weekday.take(ABBREVIATION_LENGTH) + ".",
            month = parts.month.take(ABBREVIATION_LENGTH),
        )
    }

    /** Day in full for TalkBack: "sábado", 15, "novembro", 20:00. */
    fun fullDate(startsAt: Instant): EventDateParts {
        val local = startsAt.atZone(zoneId)
        return EventDateParts(
            weekday = weekdayName(local.dayOfWeek),
            day = local.dayOfMonth,
            month = monthName(local.monthValue),
            hour = local.hour,
            minute = local.minute,
        )
    }

    /** Price on screen: "R$ 1.250,00" with a no-break space so the symbol never wraps away from the digits. */
    fun price(money: Money): String =
        CURRENCY_SYMBOL + NO_BREAK_SPACE +
            DecimalFormat(PRICE_PATTERN, DecimalFormatSymbols(PT_BR))
                .format(BigDecimal.valueOf(money.cents, CENTS_SCALE))

    /** Price split for TalkBack. */
    fun spokenPrice(money: Money): SpokenAmount {
        val reais = money.cents / CENTS_PER_REAL
        return SpokenAmount(
            reais = reais,
            reaisText = NumberFormat.getIntegerInstance(PT_BR).format(reais),
            centavos = (money.cents % CENTS_PER_REAL).toInt(),
        )
    }

    private fun monthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL, PT_BR).lowercase(PT_BR)

    private fun weekdayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, PT_BR).lowercase(PT_BR)
}
