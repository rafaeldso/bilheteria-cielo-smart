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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * SCR-04: shows the sales recorded on this device (BR-18, BR-20, BR-21).
 *
 * Ordering and filtering of the sales come from [ObservePurchaseHistoryUseCase]; this class only maps the
 * result to [PurchaseHistoryUiState]. Opening a receipt is navigation by id handled by the Route
 * ([PurchaseHistoryUiEvent.OnPurchaseSelected] does not change the state).
 */
@HiltViewModel
internal class PurchaseHistoryViewModel
    @Inject
    constructor(
        private val observeHistory: ObservePurchaseHistoryUseCase,
    ) : ViewModel() {
        private val attempt = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<PurchaseHistoryUiState> =
            attempt
                .flatMapLatest { load() }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    PurchaseHistoryUiState.Loading,
                )

        fun onEvent(event: PurchaseHistoryUiEvent) {
            when (event) {
                // "Tentar de novo" is only offered in Error (BR-21); the state is read from what the operator sees.
                PurchaseHistoryUiEvent.OnRetry -> {
                    if (uiState.value == PurchaseHistoryUiState.Error) attempt.update { it + 1 }
                }

                is PurchaseHistoryUiEvent.OnPurchaseSelected -> {
                    Unit
                }
            }
        }

        private fun load(): Flow<PurchaseHistoryUiState> =
            observeHistory()
                .map { result ->
                    when (result) {
                        is DataResult.Success -> result.value.toUiState()
                        is DataResult.Failure -> PurchaseHistoryUiState.Error
                    }
                }.onStart { emit(PurchaseHistoryUiState.Loading) }

        private fun List<Purchase>.toUiState(): PurchaseHistoryUiState =
            if (isEmpty()) {
                PurchaseHistoryUiState.Empty
            } else {
                PurchaseHistoryUiState.Content(map { it.toHistoryItem() }.toImmutableList())
            }

        private fun Purchase.toHistoryItem() =
            HistoryItem(
                id = id,
                eventName = eventName,
                createdAt = createdAt,
                quantity = quantity,
                total = total,
                status = status,
            )
    }
