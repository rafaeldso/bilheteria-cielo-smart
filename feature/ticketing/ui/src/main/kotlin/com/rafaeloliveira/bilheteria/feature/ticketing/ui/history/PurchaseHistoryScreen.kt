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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsEmptyState
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsErrorState
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsIconButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeleton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsTopAppBar
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.ScrollableState
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.toSaleResult
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant
import java.time.ZoneId

private const val ITEM_CONTENT_TYPE = "purchase"
private const val SECTION_CONTENT_TYPE = "count"

/**
 * SCR-04: the sales recorded on this device (US-08). Stateless: draws every [PurchaseHistoryUiState] and emits
 * [PurchaseHistoryUiEvent]; going back is [onNavigateUp] because navigation is not a UI event.
 *
 * @param now moment used to write dates relative to today ("hoje", "ontem").
 * @param zone time zone of the dates shown.
 */
@Composable
internal fun PurchaseHistoryScreen(
    uiState: PurchaseHistoryUiState,
    now: Instant,
    zone: ZoneId,
    onEvent: (PurchaseHistoryUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HistoryTopBar(onNavigateUp = onNavigateUp)
            when (uiState) {
                PurchaseHistoryUiState.Loading -> {
                    DsLoadingSkeleton(contentDescription = stringResource(R.string.ticketing_history_loading))
                }

                is PurchaseHistoryUiState.Content -> {
                    HistoryList(items = uiState.items, now = now, zone = zone, onEvent = onEvent)
                }

                // Scrolls so the title, text and button stay reachable at 200% font, clear of the navigation bar.
                PurchaseHistoryUiState.Empty -> {
                    ScrollableState {
                        DsEmptyState(
                            title = stringResource(R.string.ticketing_history_empty_title),
                            body = stringResource(R.string.ticketing_history_empty_body),
                            icon = DsIcons.ReceiptLong,
                        )
                    }
                }

                PurchaseHistoryUiState.Error -> {
                    ScrollableState {
                        DsErrorState(
                            title = stringResource(R.string.ticketing_history_error_title),
                            body = stringResource(R.string.ticketing_history_error_body),
                            retryLabel = stringResource(R.string.ticketing_common_retry),
                            onRetryClick = { onEvent(PurchaseHistoryUiEvent.OnRetry) },
                            icon = DsIcons.History,
                        )
                    }
                }
            }
        }
    }
}

/** The bar grows with the font, so "Histórico de compras" is whole at 200% (it wraps to two lines). */
@Composable
private fun HistoryTopBar(onNavigateUp: () -> Unit) {
    DsTopAppBar(
        title = stringResource(R.string.ticketing_history_title),
        navigationIcon = {
            DsIconButton(
                icon = DsIcons.ArrowBack,
                contentDescription = stringResource(R.string.ticketing_history_back),
                onClick = onNavigateUp,
            )
        },
    )
}

@Composable
private fun HistoryList(
    items: ImmutableList<HistoryItem>,
    now: Instant,
    zone: ZoneId,
    onEvent: (PurchaseHistoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        item(key = SECTION_CONTENT_TYPE, contentType = SECTION_CONTENT_TYPE) {
            Text(
                text = pluralStringResource(R.plurals.ticketing_history_count, items.size, items.size),
                modifier =
                    Modifier.padding(
                        start = spacing.lg,
                        top = spacing.md,
                        end = spacing.lg,
                        bottom = spacing.xs,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        itemsIndexed(items, key = { _, item -> item.id }, contentType = { _, _ -> ITEM_CONTENT_TYPE }) { index, item ->
            // A status that is not a sale never reaches here (BR-18); it is skipped rather than mislabeled.
            val status = item.status.toSaleResult()
            if (status != null) {
                HistoryItemRow(
                    item = item,
                    status = status,
                    now = now,
                    zone = zone,
                    showTopDivider = index > 0,
                    onClick = { onEvent(PurchaseHistoryUiEvent.OnPurchaseSelected(item.id)) },
                )
            }
        }
    }
}
