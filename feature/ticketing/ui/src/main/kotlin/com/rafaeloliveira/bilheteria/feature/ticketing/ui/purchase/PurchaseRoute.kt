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

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * SCR-02 Compra, stateful: the only place that knows the ViewModel and the navigation callbacks.
 *
 * @param onCompleted the purchase reached a result; opens the receipt of the given purchase id and must remove this
 * screen from the back stack, so back from the receipt does not reopen the purchase.
 * @param onBackToEvents "Voltar para eventos" of the `Unavailable` state.
 * @param onNavigateUp the back arrow of the top app bar.
 */
@Composable
internal fun PurchaseRoute(
    onCompleted: (purchaseId: String) -> Unit,
    onBackToEvents: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    // An open payment or its confirmation must not be abandoned (BR-10, BR-15, BR-25).
    BackHandler(enabled = uiState.blocksSystemBack()) { /* ignored on purpose */ }

    val completedId = (uiState as? PurchaseUiState.Completed)?.purchaseId
    LaunchedEffect(completedId) {
        if (completedId != null) currentOnCompleted(completedId)
    }

    PurchaseScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
        onBackToEvents = onBackToEvents,
        modifier = modifier,
    )
}
