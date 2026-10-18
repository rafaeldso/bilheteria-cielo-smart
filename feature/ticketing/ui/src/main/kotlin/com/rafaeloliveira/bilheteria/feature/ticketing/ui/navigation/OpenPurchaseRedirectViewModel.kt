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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveOpenPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides, once per activity, whether the app starts by resuming an open purchase (BR-15): the purchase screen then
 * recovers it (`Recovering`) before any new charge. A failure to read the open purchase is not a redirect: the user
 * lands on SCR-01 and the purchase screen, which reads it again, handles the failure (BR-26).
 *
 * The redirect is a navigation state with a consumption event ([OpenPurchaseRedirectUiEvent.OnRedirectHandled]), so
 * it is not repeated when the activity is recreated or the user later comes back to SCR-01 (AP-23). The ViewModel is
 * scoped to the activity.
 */
@HiltViewModel
internal class OpenPurchaseRedirectViewModel
    @Inject
    constructor(
        private val observeOpenPurchase: ObserveOpenPurchaseUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<OpenPurchaseRedirectUiState>(OpenPurchaseRedirectUiState.Checking)
        val uiState: StateFlow<OpenPurchaseRedirectUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val result = observeOpenPurchase().firstOrNull()
                _uiState.value =
                    when (result) {
                        is DataResult.Success -> {
                            result.value?.let { OpenPurchaseRedirectUiState.ToPurchase(it.eventId) }
                                ?: OpenPurchaseRedirectUiState.None
                        }

                        is DataResult.Failure, null -> {
                            OpenPurchaseRedirectUiState.None
                        }
                    }
            }
        }

        fun onEvent(event: OpenPurchaseRedirectUiEvent) {
            when (event) {
                OpenPurchaseRedirectUiEvent.OnRedirectHandled -> _uiState.value = OpenPurchaseRedirectUiState.None
            }
        }
    }
