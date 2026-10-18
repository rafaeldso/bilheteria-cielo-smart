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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.eventlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsEmptyState
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsErrorState
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsIconButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeleton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsTopAppBar
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R

/**
 * SCR-01 "Eventos à venda" (UI Spec 4): stateless, one explicit branch per [EventListUiState].
 *
 * @param uiState what to draw.
 * @param formats how dates and prices are written (pt-BR, display zone).
 * @param onEvent receives the user actions of the spec; navigation is handled by the caller (the route).
 */
@Composable
internal fun EventListScreen(
    uiState: EventListUiState,
    formats: EventListFormats,
    onEvent: (EventListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column {
            DsTopAppBar(
                title = stringResource(R.string.ticketing_events_title),
                actions = {
                    DsIconButton(
                        icon = DsIcons.ReceiptLong,
                        contentDescription = stringResource(R.string.ticketing_events_history),
                        onClick = { onEvent(EventListUiEvent.OnOpenHistory) },
                    )
                },
            )
            when (uiState) {
                EventListUiState.Loading -> {
                    DsLoadingSkeleton(contentDescription = stringResource(R.string.ticketing_events_loading))
                }

                is EventListUiState.Content -> {
                    EventListContent(content = uiState, formats = formats, onEvent = onEvent)
                }

                EventListUiState.Empty -> {
                    EventListEmpty()
                }

                EventListUiState.Error -> {
                    EventListError(onRetryClick = { onEvent(EventListUiEvent.OnRetry) })
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.EventListEmpty() {
    ScrollableState {
        DsEmptyState(
            title = stringResource(R.string.ticketing_events_empty_title),
            body = stringResource(R.string.ticketing_events_empty_body),
            icon = DsIcons.Event,
        )
    }
}

@Composable
private fun ColumnScope.EventListError(onRetryClick: () -> Unit) {
    ScrollableState {
        DsErrorState(
            title = stringResource(R.string.ticketing_events_error_title),
            body = stringResource(R.string.ticketing_events_error_body),
            retryLabel = stringResource(R.string.ticketing_events_retry),
            onRetryClick = onRetryClick,
            icon = DsIcons.EventBusy,
        )
    }
}

/** Empty and error messages are short, but at 200% font they can outgrow a small screen: let them scroll. */
@Composable
private fun ColumnScope.ScrollableState(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        content()
    }
}
