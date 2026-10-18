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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsErrorState
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsIconButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsKeyValueList
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeleton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeletonVariant
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusBadge
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsTopAppBar
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.SaleResult
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenMoney
import java.time.ZoneId

/**
 * Receipt of a purchase (SCR-03), stateless: renders the [uiState] and sends the operator's actions to [onEvent].
 * Every state of the spec has its own branch: [ReceiptUiState.Loading], the three results and
 * [ReceiptUiState.Error]. The screen scrolls, so nothing is cut at 200% font.
 *
 * Dates are absolute and in pt-BR, in [zoneId] (the receipt can be reopened days later). The screen never reads the
 * clock: the Route decides the zone.
 *
 * @param origin sale shows "Nova venda" and announces the result; history shows the back arrow ([onNavigateUp]) and
 * stays silent, because the operator opened the receipt on purpose.
 * @param zoneId zone used to show the instants.
 * @param onNavigateUp the back arrow of the history origin (navigation, not a [ReceiptUiEvent]).
 */
@Composable
internal fun ReceiptScreen(
    uiState: ReceiptUiState,
    origin: ReceiptOrigin,
    zoneId: ZoneId,
    onEvent: (ReceiptUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { ReceiptTopBar(origin = origin, onNavigateUp = onNavigateUp) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = BilheteriaTheme.sizes.contentMaxWidth)) {
                ReceiptBody(uiState = uiState, origin = origin, onEvent = onEvent, zoneId = zoneId)
            }
        }
    }
}

@Composable
private fun ReceiptTopBar(
    origin: ReceiptOrigin,
    onNavigateUp: () -> Unit,
) {
    DsTopAppBar(
        title = stringResource(R.string.ticketing_receipt_title),
        navigationIcon = {
            if (origin == ReceiptOrigin.History) {
                DsIconButton(
                    icon = DsIcons.ArrowBack,
                    contentDescription = stringResource(R.string.ticketing_receipt_back),
                    onClick = onNavigateUp,
                )
            }
        },
    )
}

@Composable
private fun ReceiptBody(
    uiState: ReceiptUiState,
    origin: ReceiptOrigin,
    onEvent: (ReceiptUiEvent) -> Unit,
    zoneId: ZoneId,
) {
    val fromSale = origin == ReceiptOrigin.Sale
    val onNewSaleClick = { onEvent(ReceiptUiEvent.OnNewSale) }
    when (uiState) {
        ReceiptUiState.Loading -> {
            DsLoadingSkeleton(
                contentDescription = stringResource(R.string.ticketing_receipt_loading),
                variant = DsLoadingSkeletonVariant.Card,
            )
        }

        ReceiptUiState.Error -> {
            DsErrorState(
                title = stringResource(R.string.ticketing_receipt_error_title),
                body = stringResource(R.string.ticketing_receipt_error_body),
                retryLabel = stringResource(R.string.ticketing_receipt_retry),
                onRetryClick = { onEvent(ReceiptUiEvent.OnRetry) },
                icon = DsIcons.ReceiptLong,
            )
        }

        is ReceiptUiState.Approved -> {
            ReceiptContent(SaleResult.Approved, uiState.receipt, zoneId, fromSale, onNewSaleClick)
        }

        is ReceiptUiState.Declined -> {
            ReceiptContent(SaleResult.Declined, uiState.receipt, zoneId, fromSale, onNewSaleClick)
        }

        is ReceiptUiState.Cancelled -> {
            ReceiptContent(SaleResult.Cancelled, uiState.receipt, zoneId, fromSale, onNewSaleClick)
        }
    }
}

/**
 * Reading order (ui-spec 4.6): result block, then the field list, then the way out. [fromSale] is true right after
 * the payment: only then the result is announced and "Nova venda" is offered.
 */
@Composable
private fun ReceiptContent(
    outcome: SaleResult,
    receipt: ReceiptData,
    zoneId: ZoneId,
    fromSale: Boolean,
    onNewSaleClick: () -> Unit,
) {
    val rows = remember(receipt, outcome, zoneId) { receipt.toRows(outcome, zoneId) }
    val total = remember(receipt.total) { receipt.total.toAmount() }
    Column(
        modifier = Modifier.fillMaxWidth().padding(BilheteriaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.lg),
    ) {
        ReceiptResult(outcome = outcome, total = total, announce = fromSale)
        DsKeyValueList(items = rememberKeyValues(rows))
        if (fromSale) {
            DsButton(
                text = stringResource(R.string.ticketing_receipt_new_sale),
                onClick = onNewSaleClick,
                fillMaxWidth = true,
                leadingIcon = DsIcons.AddShoppingCart,
            )
        }
    }
}

/**
 * Badge, total and result message as one block: a status (polite) for an approval, an alert (assertive) for a
 * decline or a cancellation, announced only when [announce] (right after the payment; reopening a receipt from the
 * history is silent). The total is spoken in full ("100 reais"), not as "R$ 100,00".
 */
@Composable
private fun ReceiptResult(
    outcome: SaleResult,
    total: ReceiptValue.Amount,
    announce: Boolean,
) {
    val spokenTotal = spokenMoney(total.spoken)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { if (announce) liveRegion = outcome.liveRegion },
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.sm),
    ) {
        DsStatusBadge(text = stringResource(outcome.statusRes), variant = outcome.badge)
        Text(
            text = total.formatted,
            modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(spokenTotal) },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displaySmall.tabular(),
        )
        Text(
            text = stringResource(outcome.titleRes()),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(outcome.bodyRes()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
