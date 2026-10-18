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
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The locale of every amount and date on the purchase screen (AP-40): never the device default. */
internal val PtBr: Locale = Locale.forLanguageTag("pt-BR")

private const val CENTS_SCALE = 2
private const val CENTS_PER_REAL = 100L
private const val TIME_PATTERN = "HH:mm"

/** Reais and centavos of an amount, split to be read aloud ("100 reais e 50 centavos"). */
internal data class SpokenMoney(
    val reais: Long,
    val centavos: Int,
)

/** "R$ 1.250,00": the currency format of Brazil, from the exact cents (never a floating point). */
internal fun Money.format(locale: Locale = PtBr): String =
    NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(cents, CENTS_SCALE))

internal fun Money.spoken(): SpokenMoney {
    val centavos = cents % CENTS_PER_REAL
    return SpokenMoney(reais = cents / CENTS_PER_REAL, centavos = centavos.toInt())
}

/** Short day of the event in the given zone, as in the UI Spec: "dom., 15 nov". */
internal fun Instant.formatEventDay(
    zone: ZoneId,
    locale: Locale = PtBr,
): String {
    val date = atZone(zone)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
    val month = date.month.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
    return "$weekday., ${date.dayOfMonth} $month"
}

/** Start time of the event in the given zone: "20:00". */
internal fun Instant.formatEventTime(
    zone: ZoneId,
    locale: Locale = PtBr,
): String = DateTimeFormatter.ofPattern(TIME_PATTERN, locale).format(atZone(zone))
