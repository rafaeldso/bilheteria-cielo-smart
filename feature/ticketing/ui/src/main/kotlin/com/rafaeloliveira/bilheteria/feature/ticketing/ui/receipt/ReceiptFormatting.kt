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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The receipt is always pt-BR, whatever the device locale (the terminal is operated in Brazil). */
internal val ReceiptLocale: Locale = Locale.forLanguageTag("pt-BR")

private const val CENTS_PER_REAL = 100L
private const val MONEY_SCALE = 2
private const val DATE_PATTERN = "dd/MM/yyyy"
private const val TIME_PATTERN = "HH:mm"

/** Money as shown on screen, e.g. `R$ 100,00` (BR-06: cents stay exact, never a Double). */
internal fun Money.formatForReceipt(locale: Locale = ReceiptLocale): String =
    NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(cents, MONEY_SCALE))

/**
 * Money split for the screen reader: TalkBack must say "100 reais", not "R$ 100,00". The words (real/reais,
 * centavo/centavos) come from plural resources, so only the numbers are decided here.
 */
internal data class SpokenMoney(
    val reais: Long,
    val centavos: Int,
) {
    /** Plural quantity for the reais, clamped because plural resources take an `Int`. */
    val reaisQuantity: Int get() = reais.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    /** True when the amount has both parts and they are joined by "e" ("10 reais e 50 centavos"). */
    val hasReaisAndCentavos: Boolean get() = reais > 0L && centavos > 0

    /** True when only the cents are spoken ("50 centavos"). */
    val onlyCentavos: Boolean get() = reais == 0L && centavos > 0
}

internal fun Money.toSpokenMoney(): SpokenMoney {
    val remainder = cents % CENTS_PER_REAL
    return SpokenMoney(reais = cents / CENTS_PER_REAL, centavos = remainder.toInt())
}

/** Absolute date and time of an instant, in two parts so the localized "às" stays in a string resource. */
internal data class ReceiptDateTime(
    val date: String,
    val time: String,
)

/**
 * Absolute `dd/MM/yyyy` and `HH:mm` in [zone] (BR-16, ui-spec 4.4): the receipt can be reopened days later, so
 * "hoje"/"ontem" would be wrong. The zone is explicit (no hidden default) and the locale is pt-BR.
 */
internal fun Instant.toReceiptDateTime(
    zone: ZoneId,
    locale: Locale = ReceiptLocale,
): ReceiptDateTime =
    ReceiptDateTime(
        date = DateTimeFormatter.ofPattern(DATE_PATTERN, locale).withZone(zone).format(this),
        time = DateTimeFormatter.ofPattern(TIME_PATTERN, locale).withZone(zone).format(this),
    )
