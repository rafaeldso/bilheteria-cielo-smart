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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Clock

/**
 * Stateful entry of SCR-04: connects [PurchaseHistoryViewModel] to [PurchaseHistoryScreen].
 *
 * Opening a receipt and going back are navigation, so they leave through [onPurchaseSelected] (receives the
 * purchase id, BR-19) and [onNavigateUp] instead of the ViewModel. The other events go to the ViewModel.
 *
 * @param clock source of "now" for the relative dates; the default is the system clock and zone.
 */
@Composable
internal fun PurchaseHistoryRoute(
    onPurchaseSelected: (purchaseId: String) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseHistoryViewModel = hiltViewModel(),
    clock: Clock = Clock.systemDefaultZone(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnPurchaseSelected by rememberUpdatedState(onPurchaseSelected)
    val onEvent =
        remember(viewModel) {
            { event: PurchaseHistoryUiEvent ->
                when (event) {
                    is PurchaseHistoryUiEvent.OnPurchaseSelected -> currentOnPurchaseSelected(event.id)
                    PurchaseHistoryUiEvent.OnRetry -> viewModel.onEvent(event)
                }
            }
        }
    // Read again whenever the state changes, so a list shown after midnight is not dated against yesterday.
    val now = remember(uiState, clock) { clock.instant() }

    PurchaseHistoryScreen(
        uiState = uiState,
        now = now,
        zone = clock.zone,
        onEvent = onEvent,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}
