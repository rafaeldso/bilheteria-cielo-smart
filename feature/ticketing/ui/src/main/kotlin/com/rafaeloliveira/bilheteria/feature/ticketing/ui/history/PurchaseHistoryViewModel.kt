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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SCR-04: shows the sales recorded on this device (BR-18, BR-20, BR-21).
 *
 * Ordering and filtering of the sales come from [ObservePurchaseHistoryUseCase]; this class only maps the
 * result to [PurchaseHistoryUiState]. Opening a receipt is navigation by id handled by the Route
 * ([PurchaseHistoryUiEvent.OnPurchaseSelected] does not change the state).
 */
@HiltViewModel
class PurchaseHistoryViewModel
    @Inject
    constructor(
        private val observeHistory: ObservePurchaseHistoryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PurchaseHistoryUiState>(PurchaseHistoryUiState.Loading)
        val uiState: StateFlow<PurchaseHistoryUiState> = _uiState.asStateFlow()

        private var observing: Job? = null

        init {
            observe()
        }

        fun onEvent(event: PurchaseHistoryUiEvent) {
            when (event) {
                PurchaseHistoryUiEvent.OnRetry -> {
                    _uiState.value = PurchaseHistoryUiState.Loading
                    observe()
                }

                is PurchaseHistoryUiEvent.OnPurchaseSelected -> {
                    // Navigation by id is the Route's job; the state does not change.
                }
            }
        }

        private fun observe() {
            observing?.cancel()
            observing =
                viewModelScope.launch {
                    observeHistory().collect { result ->
                        _uiState.value =
                            when (result) {
                                is DataResult.Success -> result.value.toUiState()
                                is DataResult.Failure -> PurchaseHistoryUiState.Error
                            }
                    }
                }
        }

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
