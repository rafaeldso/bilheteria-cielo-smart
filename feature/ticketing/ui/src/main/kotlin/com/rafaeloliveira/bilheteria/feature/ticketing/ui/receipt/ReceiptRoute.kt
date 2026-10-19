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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Clock

/**
 * Stateful entry of SCR-03 (`ReceiptRoute(purchaseId)`): connects the [ReceiptViewModel] to the stateless
 * [ReceiptScreen] and runs the navigation, which is not state.
 *
 * "Nova venda" ([ReceiptUiEvent.OnNewSale]) and "Ver ingressos" ([ReceiptUiEvent.OnOpenTickets]) are intercepted
 * here and become [onNewSale] and [onOpenTickets]; every other event goes to the ViewModel. [onNavigateUp] is the
 * back arrow of the history origin.
 *
 * @param origin sale or history; decides which way out is shown.
 * @param onNewSale leaves the receipt for a new sale (back to the event list).
 * @param onOpenTickets opens the tickets of this purchase (SCR-05); only an approved receipt offers it.
 * @param onNavigateUp leaves the receipt for the history.
 * @param clock source of the zone the dates are shown in; the app's single clock.
 */
@Composable
internal fun ReceiptRoute(
    origin: ReceiptOrigin,
    onNewSale: () -> Unit,
    onOpenTickets: () -> Unit,
    onNavigateUp: () -> Unit,
    clock: Clock,
    modifier: Modifier = Modifier,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The result is read once: coming back from the tickets screen must not announce it again (UI Spec 002 §4.3).
    var announced by rememberSaveable { mutableStateOf(false) }
    val currentOnNewSale by rememberUpdatedState(onNewSale)
    val currentOnOpenTickets by rememberUpdatedState(onOpenTickets)
    val onEvent: (ReceiptUiEvent) -> Unit =
        remember(viewModel) {
            { event ->
                when (event) {
                    ReceiptUiEvent.OnNewSale -> {
                        currentOnNewSale()
                    }

                    ReceiptUiEvent.OnOpenTickets -> {
                        announced = true // the live region stays on screen and ends when the receipt is left
                        currentOnOpenTickets()
                    }

                    ReceiptUiEvent.OnRetry -> {
                        viewModel.onEvent(event)
                    }
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
        announceResult = !announced,
    )
}
