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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.formatBrl
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.fullEventDate
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.shortEventDate
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenEventDate
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenMoney
import java.time.ZoneId

/**
 * Event name, day, time, venue and unit price, read by TalkBack as one sentence with the date in full ("domingo, 15
 * de novembro, às 20 horas") and the price in words. The day and time are in [zone], where the terminal is (the
 * event happens next to it); the caller decides it, the header never reads the clock.
 */
@Composable
internal fun EventHeader(
    event: EventSummary,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    val shortDate = remember(event.startsAt, zone) { event.startsAt.shortEventDate(zone) }
    val unitPrice = remember(event.unitPrice) { event.unitPrice.formatBrl() }
    val spokenLabel = eventSpokenLabel(event = event, zone = zone)
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spokenLabel },
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.xs),
    ) {
        Text(
            text = event.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                stringResource(
                    R.string.ticketing_purchase_event_when,
                    shortDate.weekday,
                    shortDate.day,
                    shortDate.month,
                    shortDate.time,
                    event.venue,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = unitPriceLine(unitPrice, event.available),
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Noite de Jazz, domingo, 15 de novembro, às 20 horas, Concha, 50 reais por ingresso · 48 disponíveis". */
@Composable
private fun eventSpokenLabel(
    event: EventSummary,
    zone: ZoneId,
): String {
    val fullDate = remember(event.startsAt, zone) { event.startsAt.fullEventDate(zone) }
    return stringResource(
        R.string.ticketing_purchase_event_a11y,
        event.name,
        spokenEventDate(fullDate),
        event.venue,
        unitPriceLine(spokenMoney(event.unitPrice), event.available),
    )
}

/** The stock is not part of the purchase snapshot, so the line only carries the price then (`available` null). */
@Composable
private fun unitPriceLine(
    unitPrice: String,
    available: Int?,
): String =
    if (available == null) {
        stringResource(R.string.ticketing_purchase_unit_price_only, unitPrice)
    } else {
        stringResource(
            R.string.ticketing_purchase_unit_price,
            unitPrice,
            pluralStringResource(R.plurals.ticketing_purchase_available, available, available),
        )
    }

/** "2 ingressos". */
@Composable
internal fun ticketsText(count: Int): String = pluralStringResource(R.plurals.ticketing_purchase_tickets, count, count)
