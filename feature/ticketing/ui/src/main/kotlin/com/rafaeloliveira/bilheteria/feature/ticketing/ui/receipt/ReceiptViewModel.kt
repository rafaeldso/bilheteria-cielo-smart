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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** `ReceiptRoute(purchaseId)` argument in the [SavedStateHandle]: type-safe navigation uses the property name. */
private const val PURCHASE_ID_KEY = "purchaseId"
private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Receipt of a purchase (SCR-03; BR-11, BR-12, BR-13, BR-16, BR-19). The screen is reached right after the payment
 * result or from the history, and both cases are the same state: the local record, observed by id (Room is the source
 * of truth). A record that is not a result yet (open or failed purchase) or cannot be read stays in
 * [ReceiptUiState.Loading]: the plan has no error state for this screen.
 */
@HiltViewModel
class ReceiptViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observePurchase: ObservePurchaseUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<ReceiptUiState> =
            purchaseFlow(savedStateHandle, observePurchase)
                .map { result -> result.toUiState() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReceiptUiState.Loading)

        /** "Nova venda" changes no state: leaving the screen is navigation, executed by the Route. */
        fun onEvent(event: ReceiptUiEvent) {
            when (event) {
                ReceiptUiEvent.OnNewSale -> Unit
            }
        }
    }

private fun purchaseFlow(
    savedStateHandle: SavedStateHandle,
    observePurchase: ObservePurchaseUseCase,
): Flow<DataResult<Purchase>> =
    savedStateHandle.get<String>(PURCHASE_ID_KEY)?.let { id -> observePurchase(id) } ?: emptyFlow()

private fun DataResult<Purchase>.toUiState(): ReceiptUiState =
    when (this) {
        is DataResult.Failure -> ReceiptUiState.Loading
        is DataResult.Success -> value.toUiState()
    }

private fun Purchase.toUiState(): ReceiptUiState =
    when (status) {
        PurchaseStatus.Approved -> ReceiptUiState.Approved(toReceipt(transaction?.toReceiptTransaction()))

        PurchaseStatus.Declined -> ReceiptUiState.Declined(toReceipt(transaction = null))

        PurchaseStatus.Cancelled -> ReceiptUiState.Cancelled(toReceipt(transaction = null))

        PurchaseStatus.Pending,
        PurchaseStatus.Recovering,
        PurchaseStatus.Failed,
        -> ReceiptUiState.Loading
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
