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
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Every date and amount of SCR-04 is formatted for Brazil, whatever the device locale. */
private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", PtBr)
private val DayMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", PtBr)

private const val CENTS_PER_REAL = 100
private const val CENTS_SCALE = 2
private const val NO_BREAK_SPACE = ' '

/** Amount as `R$ 1.250,00`. The space is non-breaking so the currency never wraps away from the figures. */
internal fun formatMoney(money: Money): String {
    val figures =
        NumberFormat
            .getNumberInstance(PtBr)
            .apply {
                minimumFractionDigits = CENTS_SCALE
                maximumFractionDigits = CENTS_SCALE
            }.format(BigDecimal.valueOf(money.cents, CENTS_SCALE))
    return "R\$$NO_BREAK_SPACE$figures"
}

/** Whole reais and leftover cents of an amount, to be spelled out for TalkBack ("100 reais e 50 centavos"). */
internal data class SpokenMoney(
    val reais: Int,
    val cents: Int,
) {
    val hasReais: Boolean get() = reais > 0 || cents == 0
    val hasCents: Boolean get() = cents > 0
}

internal fun Money.toSpokenMoney(): SpokenMoney =
    SpokenMoney(
        reais = (cents / CENTS_PER_REAL).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        cents = (cents % CENTS_PER_REAL).toInt(),
    )

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

/** The three results shown in the history (BR-18). */
internal enum class HistoryStatus { Approved, Declined, Cancelled }

/**
 * The status of a sale, or null for a status that is not a sale. The use case already keeps only sales (BR-18),
 * so null is a defensive branch: the row is not drawn instead of being shown with a wrong label.
 */
internal fun PurchaseStatus.toHistoryStatus(): HistoryStatus? =
    when (this) {
        PurchaseStatus.Approved -> HistoryStatus.Approved
        PurchaseStatus.Declined -> HistoryStatus.Declined
        PurchaseStatus.Cancelled -> HistoryStatus.Cancelled
        PurchaseStatus.Pending, PurchaseStatus.Recovering, PurchaseStatus.Failed -> null
    }

/** Color and icon of the badge for each result: green, red and neutral (ui-spec SCR-03/SCR-04, 4.3). */
internal fun HistoryStatus.badgeVariant(): DsStatusBadgeVariant =
    when (this) {
        HistoryStatus.Approved -> DsStatusBadgeVariant.Success
        HistoryStatus.Declined -> DsStatusBadgeVariant.Error
        HistoryStatus.Cancelled -> DsStatusBadgeVariant.Neutral
    }
