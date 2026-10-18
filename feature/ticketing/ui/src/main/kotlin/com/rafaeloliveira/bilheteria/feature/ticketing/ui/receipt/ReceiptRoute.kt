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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Clock

/**
 * Stateful entry of SCR-03 (`ReceiptRoute(purchaseId)`): connects the [ReceiptViewModel] to the stateless
 * [ReceiptScreen] and runs the navigation, which is not state.
 *
 * "Nova venda" ([ReceiptUiEvent.OnNewSale]) is intercepted here and becomes [onNewSale]; every other event goes
 * to the ViewModel. [onNavigateUp] is the back arrow of the history origin.
 *
 * @param origin sale or history; decides which way out is shown.
 * @param onNewSale leaves the receipt for a new sale (back to the event list).
 * @param onNavigateUp leaves the receipt for the history.
 * @param clock source of the zone the dates are shown in; the default is the system clock and zone.
 */
@Composable
internal fun ReceiptRoute(
    origin: ReceiptOrigin,
    onNewSale: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptViewModel = hiltViewModel(),
    clock: Clock = Clock.systemDefaultZone(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNewSale by rememberUpdatedState(onNewSale)
    val onEvent: (ReceiptUiEvent) -> Unit =
        remember(viewModel) {
            { event ->
                when (event) {
                    ReceiptUiEvent.OnNewSale -> currentOnNewSale()
                    ReceiptUiEvent.OnRetry -> viewModel.onEvent(event)
                }
            }
        }
    ReceiptScreen(
        uiState = uiState,
        origin = origin,
        zoneId = clock.zone,
        onEvent = onEvent,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}
