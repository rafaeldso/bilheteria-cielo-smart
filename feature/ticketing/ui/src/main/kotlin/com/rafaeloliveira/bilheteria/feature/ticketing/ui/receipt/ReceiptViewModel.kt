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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.ReceiptRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

/**
 * `ReceiptRoute(purchaseId)` argument in the [SavedStateHandle]: type-safe navigation names the entry after the route
 * property, so the key is read from the route itself and cannot drift from it.
 */
private val PURCHASE_ID_KEY: String = ReceiptRoute.serializer().descriptor.getElementName(0)
private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Receipt of a purchase (SCR-03; BR-11, BR-12, BR-13, BR-16, BR-19, BR-28). The screen is reached right after the
 * payment result or from the history, and both cases are the same state: the local record, observed by id (Room is the
 * source of truth). A record still open (`Pending`/`Recovering`) is the transient [ReceiptUiState.Loading]; a record
 * that cannot be read, does not exist or ended `Failed` is a terminal [ReceiptUiState.Error] with a retry (BR-28).
 */
@HiltViewModel
internal class ReceiptViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val observePurchase: ObservePurchaseUseCase,
    ) : ViewModel() {
        // A receipt without its route argument is a programming error, not a screen state.
        private val purchaseId: String = checkNotNull(savedStateHandle.get<String>(PURCHASE_ID_KEY))
        private val attempt = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ReceiptUiState> =
            attempt
                .flatMapLatest { load() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReceiptUiState.Loading)

        /** "Nova venda" changes no state: leaving the screen is navigation, executed by the Route. */
        fun onEvent(event: ReceiptUiEvent) {
            when (event) {
                ReceiptUiEvent.OnNewSale -> Unit

                // Read from the state the operator is looking at (the Error button is only shown in it).
                ReceiptUiEvent.OnRetry -> if (uiState.value == ReceiptUiState.Error) attempt.update { it + 1 }
            }
        }

        private fun load(): Flow<ReceiptUiState> =
            observePurchase(purchaseId)
                .map { result -> result.toUiState() }
                .onStart { emit(ReceiptUiState.Loading) }
    }

private fun DataResult<Purchase>.toUiState(): ReceiptUiState =
    when (this) {
        is DataResult.Failure -> ReceiptUiState.Error
        is DataResult.Success -> value.toUiState()
    }

private fun Purchase.toUiState(): ReceiptUiState =
    when (status) {
        PurchaseStatus.Approved -> ReceiptUiState.Approved(toReceipt(transaction?.toReceiptTransaction()))
        PurchaseStatus.Declined -> ReceiptUiState.Declined(toReceipt(transaction = null))
        PurchaseStatus.Cancelled -> ReceiptUiState.Cancelled(toReceipt(transaction = null))
        PurchaseStatus.Pending, PurchaseStatus.Recovering -> ReceiptUiState.Loading
        PurchaseStatus.Failed -> ReceiptUiState.Error
    }

/** Transaction data only goes to the receipt of an approved purchase (AC-16). */
private fun Purchase.toReceipt(transaction: ReceiptTransaction?): ReceiptData =
    ReceiptData(
        purchaseNumber = reference.value,
        eventName = eventName,
        eventStartsAt = eventStartsAt,
        venue = venue,
        quantity = quantity,
        unitPrice = unitPrice,
        total = total,
        purchasedAt = createdAt,
        transaction = transaction,
    )

private fun TransactionData.toReceiptTransaction(): ReceiptTransaction =
    ReceiptTransaction(brand = brand, authCode = authCode, nsu = nsu)
