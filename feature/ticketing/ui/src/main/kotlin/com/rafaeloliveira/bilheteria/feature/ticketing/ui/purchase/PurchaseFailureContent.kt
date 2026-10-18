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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsErrorState
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.formatBrl
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenMoney
import java.time.ZoneId

/**
 * `Error`: what failed, the quantity and total of the purchase, and the retry. The error is an alert and the focus
 * goes to the button (DsErrorState). "Consultar de novo" only repeats the lookup (BR-26); the others pay again.
 */
@Composable
internal fun ErrorContent(
    state: PurchaseUiState.Error,
    zone: ZoneId,
    onEvent: (PurchaseUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = remember(state.total) { state.total.formatBrl() }
    val retryLabel =
        when (state.reason.retryAction()) {
            RetryAction.PayAgain -> stringResource(R.string.ticketing_purchase_retry_pay, total)
            RetryAction.CheckAgain -> stringResource(R.string.ticketing_purchase_retry_recovery)
        }
    val tickets = ticketsText(state.quantity)
    val summary = stringResource(R.string.ticketing_purchase_error_summary, tickets, total)
    val spokenSummary = stringResource(R.string.ticketing_purchase_error_summary, tickets, spokenMoney(state.total))
    Column(modifier = modifier) {
        EventHeader(event = state.event, zone = zone, modifier = Modifier.padding(BilheteriaTheme.spacing.lg))
        DsErrorState(
            title = stringResource(state.reason.titleRes()),
            body = stringResource(state.reason.bodyRes()),
            retryLabel = retryLabel,
            onRetryClick = { onEvent(PurchaseUiEvent.OnRetry) },
            icon = state.reason.icon(),
            supportingContent = {
                Text(
                    text = summary,
                    modifier = Modifier.semantics { contentDescription = spokenSummary },
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

/** `Unavailable`: the event cannot be loaded and no purchase is open; no quantity and no "Pagar" (BR-27, AC-26). */
@Composable
internal fun UnavailableContent(
    onBackToEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DsErrorState(
        title = stringResource(R.string.ticketing_purchase_unavailable_title),
        body = stringResource(R.string.ticketing_purchase_unavailable_body),
        retryLabel = stringResource(R.string.ticketing_purchase_unavailable_back),
        onRetryClick = onBackToEvents,
        modifier = modifier,
        icon = DsIcons.EventBusy,
    )
}

/** The design system has no dedicated glyph for each case yet; the closest available one is used. */
private fun PurchaseErrorReason.icon(): ImageVector =
    when (this) {
        PurchaseErrorReason.NoCieloApp -> DsIcons.Block
        PurchaseErrorReason.Generic -> DsIcons.Error
        PurchaseErrorReason.Authentication -> DsIcons.Lock
        PurchaseErrorReason.RecoveryNotFound -> DsIcons.History
        PurchaseErrorReason.RecoveryFailed -> DsIcons.Sync
    }
