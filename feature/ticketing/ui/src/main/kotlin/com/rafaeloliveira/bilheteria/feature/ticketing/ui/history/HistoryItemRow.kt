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

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsListItem
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusBadge
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import java.time.Instant
import java.time.ZoneId

/**
 * One purchase of the history: event name, relative date and tickets on the left; total and status badge at the
 * end (ui-spec SCR-04, 4.1). The whole row is a single button read by TalkBack as one sentence.
 */
@Composable
internal fun HistoryItemRow(
    item: HistoryItem,
    status: HistoryStatus,
    now: Instant,
    zone: ZoneId,
    showTopDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moment = remember(item.createdAt, now, zone) { historyMoment(item.createdAt, now, zone) }
    val total = remember(item.total) { formatMoney(item.total) }
    val quantity = pluralStringResource(R.plurals.ticketing_history_quantity, item.quantity, item.quantity)
    val statusText = stringResource(status.label())
    DsListItem(
        headline = item.eventName,
        modifier = modifier,
        supportingText = stringResource(R.string.ticketing_history_item_supporting, moment.text(), quantity),
        leadingIcon = status.icon(),
        onClick = onClick,
        spokenLabel =
            stringResource(
                R.string.ticketing_history_item_a11y,
                item.eventName,
                moment.spokenText(),
                quantity,
                item.total.toSpokenMoney().spokenText(),
                statusText,
            ),
        showTopDivider = showTopDivider,
    ) {
        Text(
            text = total,
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        DsStatusBadge(text = statusText, variant = status.badgeVariant())
    }
}

@StringRes
private fun HistoryStatus.label(): Int =
    when (this) {
        HistoryStatus.Approved -> R.string.ticketing_history_status_approved
        HistoryStatus.Declined -> R.string.ticketing_history_status_declined
        HistoryStatus.Cancelled -> R.string.ticketing_history_status_cancelled
    }

private fun HistoryStatus.icon(): ImageVector =
    when (this) {
        HistoryStatus.Approved -> DsIcons.CheckCircle
        HistoryStatus.Declined -> DsIcons.Block
        HistoryStatus.Cancelled -> DsIcons.Cancel
    }

/** "hoje às 14:32", "ontem às 18:10" or "15/10 às 09:00". */
@Composable
private fun HistoryMoment.text(): String =
    when (this) {
        is HistoryMoment.Today -> stringResource(R.string.ticketing_history_datetime_today, time)
        is HistoryMoment.Yesterday -> stringResource(R.string.ticketing_history_datetime_yesterday, time)
        is HistoryMoment.OtherDay -> stringResource(R.string.ticketing_history_datetime_other, shortDate, time)
    }

/** Same as [text], but older dates are spelled out ("15 de outubro às 09:00") so TalkBack does not read "barra". */
@Composable
private fun HistoryMoment.spokenText(): String =
    when (this) {
        is HistoryMoment.Today, is HistoryMoment.Yesterday -> {
            text()
        }

        is HistoryMoment.OtherDay -> {
            stringResource(R.string.ticketing_history_datetime_other_spoken, day, monthName, time)
        }
    }

/** "100 reais", "1 real e 5 centavos" or "50 centavos". */
@Composable
private fun SpokenMoney.spokenText(): String {
    val reaisText = if (hasReais) pluralStringResource(R.plurals.ticketing_history_spoken_reais, reais, reais) else ""
    val centsText = if (hasCents) pluralStringResource(R.plurals.ticketing_history_spoken_cents, cents, cents) else ""
    return when {
        hasReais && hasCents -> stringResource(R.string.ticketing_history_spoken_reais_and_cents, reaisText, centsText)
        hasReais -> reaisText
        else -> centsText
    }
}
