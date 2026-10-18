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
import androidx.compose.ui.semantics.semantics
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import java.time.ZoneId

/**
 * Event name, day, time, venue and unit price, read by TalkBack as one block. The day and time are in the device
 * zone, where the terminal is (the event happens next to it).
 */
@Composable
internal fun EventHeader(
    event: EventSummary,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val day = remember(event.startsAt, zone) { event.startsAt.formatEventDay(zone) }
    val time = remember(event.startsAt, zone) { event.startsAt.formatEventTime(zone) }
    val unitPrice = remember(event.unitPrice) { event.unitPrice.format() }
    Column(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.xs),
    ) {
        Text(
            text = event.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.ticketing_purchase_event_when, day, time, event.venue),
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

/** An amount as TalkBack should say it: "100 reais", "100 reais e 50 centavos". */
@Composable
internal fun spokenMoney(money: Money): String {
    val parts = money.spoken()
    val reais = minOf(parts.reais, Int.MAX_VALUE.toLong()).toInt()
    return when {
        parts.centavos == 0 -> {
            pluralStringResource(R.plurals.ticketing_purchase_spoken_reais, reais, reais)
        }

        parts.reais == 0L -> {
            pluralStringResource(R.plurals.ticketing_purchase_spoken_centavos, parts.centavos, parts.centavos)
        }

        else -> {
            stringResource(
                R.string.ticketing_purchase_spoken_reais_and_centavos,
                pluralStringResource(R.plurals.ticketing_purchase_spoken_reais, reais, reais),
                pluralStringResource(R.plurals.ticketing_purchase_spoken_centavos, parts.centavos, parts.centavos),
            )
        }
    }
}
