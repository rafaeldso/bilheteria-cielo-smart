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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsIconButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeleton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeletonVariant
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsTopAppBar
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import java.time.ZoneId

/**
 * SCR-02 Compra, stateless: renders every [PurchaseUiState] and reports user intent. [onNavigateUp] and
 * [onBackToEvents] are navigation, not events (ui-spec section 3); the Route owns the system back.
 * The content scrolls so nothing is cut at 200% font.
 *
 * @param zone zone the event day and time are shown in; the screen never reads the clock, the Route decides it.
 */
@Composable
internal fun PurchaseScreen(
    uiState: PurchaseUiState,
    zone: ZoneId,
    onEvent: (PurchaseUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
    onBackToEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PurchaseTopBar(showsNavigateUp = uiState.showsNavigateUp(), onNavigateUp = onNavigateUp) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
            PurchaseBody(uiState = uiState, zone = zone, onEvent = onEvent, onBackToEvents = onBackToEvents)
        }
    }
}

@Composable
private fun PurchaseTopBar(
    showsNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
) {
    DsTopAppBar(
        title = stringResource(R.string.ticketing_purchase_title),
        navigationIcon = {
            if (showsNavigateUp) {
                DsIconButton(
                    icon = DsIcons.ArrowBack,
                    contentDescription = stringResource(R.string.ticketing_purchase_back),
                    onClick = onNavigateUp,
                )
            }
        },
    )
}

@Composable
private fun PurchaseBody(
    uiState: PurchaseUiState,
    zone: ZoneId,
    onEvent: (PurchaseUiEvent) -> Unit,
    onBackToEvents: () -> Unit,
) {
    when (uiState) {
        // `Completed` only lasts until the Route has navigated to the receipt: it keeps the loading look.
        PurchaseUiState.Loading, is PurchaseUiState.Completed -> {
            DsLoadingSkeleton(
                contentDescription = stringResource(R.string.ticketing_purchase_loading),
                variant = DsLoadingSkeletonVariant.Card,
            )
        }

        is PurchaseUiState.Editing -> {
            EditingContent(state = uiState, zone = zone, onEvent = onEvent)
        }

        is PurchaseUiState.AwaitingPayment -> {
            AwaitingPaymentContent(state = uiState, zone = zone, onEvent = onEvent)
        }

        is PurchaseUiState.Recovering -> {
            RecoveringContent()
        }

        is PurchaseUiState.Error -> {
            ErrorContent(state = uiState, zone = zone, onEvent = onEvent)
        }

        PurchaseUiState.Unavailable -> {
            UnavailableContent(onBackToEvents = onBackToEvents)
        }
    }
}
