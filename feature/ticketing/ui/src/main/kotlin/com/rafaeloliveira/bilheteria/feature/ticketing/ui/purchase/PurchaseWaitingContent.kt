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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsProgressIndicator
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusMessage
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.formatBrl
import java.time.ZoneId

/**
 * `AwaitingPayment`: the payment is open on the terminal. "Pagar" stays on screen but disabled, so a repeated tap
 * opens nothing (BR-10, AC-08); the status is announced politely.
 */
@Composable
internal fun AwaitingPaymentContent(
    state: PurchaseUiState.AwaitingPayment,
    zone: ZoneId,
    onEvent: (PurchaseUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    val total = remember(state.total) { state.total.formatBrl() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        EventHeader(event = state.event, zone = zone, modifier = Modifier.padding(spacing.lg))
        DsStatusMessage(
            icon = DsIcons.PointOfSale,
            title = stringResource(R.string.ticketing_purchase_awaiting_title),
            body = stringResource(R.string.ticketing_purchase_awaiting_body, total, ticketsText(state.quantity)),
        )
        DsProgressIndicator(
            contentDescription = stringResource(R.string.ticketing_purchase_awaiting_progress),
            modifier = Modifier.padding(horizontal = spacing.lg),
        )
        DsButton(
            text = stringResource(R.string.ticketing_purchase_awaiting_button),
            onClick = { onEvent(PurchaseUiEvent.OnPay) },
            modifier = Modifier.padding(horizontal = spacing.lg),
            enabled = false,
            fillMaxWidth = true,
            leadingIcon = DsIcons.HourglassTop,
        )
    }
}

/** `Recovering`: confirming the payment with Cielo before any new charge (BR-15, BR-25); no actions. */
@Composable
internal fun RecoveringContent(modifier: Modifier = Modifier) {
    val spacing = BilheteriaTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        DsStatusMessage(
            icon = DsIcons.Sync,
            title = stringResource(R.string.ticketing_purchase_recovering_title),
            body = stringResource(R.string.ticketing_purchase_recovering_body),
        )
        DsProgressIndicator(
            contentDescription = stringResource(R.string.ticketing_purchase_recovering_progress),
            modifier = Modifier.padding(horizontal = spacing.lg),
        )
    }
}
