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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsQuantityStepper
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsSummaryCard
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.formatBrl
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenMoney
import java.time.ZoneId

/**
 * `Editing`: event, quantity, total and "Pagar" (ui-spec SCR-02, section 4.1; AC-04 to AC-06). The event day and
 * time are shown in [zone].
 */
@Composable
internal fun EditingContent(
    state: PurchaseUiState.Editing,
    zone: ZoneId,
    onEvent: (PurchaseUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    val total = remember(state.total) { state.total.formatBrl() }
    Column(
        modifier = modifier.padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        EventHeader(event = state.event, zone = zone)
        Text(
            text = stringResource(R.string.ticketing_purchase_quantity),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QuantityStepper(state = state, onEvent = onEvent)
        state.limitHint?.let { LimitHintMessage(hint = it) }
        PurchaseTotal(unitPrice = state.event.unitPrice, quantity = state.quantity, total = state.total)
        // The amount is read in words ("Pagar 100 reais no terminal"); only the description changes, so the button
        // keeps its role and its click action.
        val spokenPay = stringResource(R.string.ticketing_purchase_pay, spokenMoney(state.total))
        DsButton(
            text = stringResource(R.string.ticketing_purchase_pay, total),
            onClick = { onEvent(PurchaseUiEvent.OnPay) },
            modifier = Modifier.semantics { contentDescription = spokenPay },
            fillMaxWidth = true,
            leadingIcon = DsIcons.PointOfSale,
        )
    }
}

/** The stepper is a group named "Quantidade de ingressos"; a button at its limit renders disabled (AC-06). */
@Composable
private fun QuantityStepper(
    state: PurchaseUiState.Editing,
    onEvent: (PurchaseUiEvent) -> Unit,
) {
    val group = stringResource(R.string.ticketing_purchase_quantity_group)
    DsQuantityStepper(
        value = state.quantity,
        onDecrement = { onEvent(PurchaseUiEvent.OnDecrement) },
        onIncrement = { onEvent(PurchaseUiEvent.OnIncrement) },
        canDecrement = state.canDecrement,
        canIncrement = state.canIncrement,
        valueDescription = ticketsText(state.quantity),
        decrementDescription = stringResource(R.string.ticketing_purchase_decrease),
        incrementDescription = stringResource(R.string.ticketing_purchase_increase),
        modifier = Modifier.semantics { contentDescription = group },
    )
}

/** Why "+" is disabled; announced politely when it appears (AC-06). */
@Composable
private fun LimitHintMessage(hint: LimitHint) {
    val text =
        when (val content = hint.text()) {
            is LimitHintText.Fixed -> stringResource(content.resId, content.value)
            is LimitHintText.Counted -> pluralStringResource(content.resId, content.count, content.count)
        }
    Text(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The dominant figure of the screen, read as "Total 100 reais, 2 ingressos de 50 reais". */
@Composable
private fun PurchaseTotal(
    unitPrice: Money,
    quantity: Int,
    total: Money,
) {
    val tickets = ticketsText(quantity)
    val unit = remember(unitPrice) { unitPrice.formatBrl() }
    DsSummaryCard(
        label = stringResource(R.string.ticketing_purchase_total),
        value = remember(total) { total.formatBrl() },
        supportingText = stringResource(R.string.ticketing_purchase_total_detail, tickets, unit),
        spokenLabel =
            stringResource(
                R.string.ticketing_purchase_total_spoken,
                spokenMoney(total),
                tickets,
                spokenMoney(unitPrice),
            ),
    )
}
