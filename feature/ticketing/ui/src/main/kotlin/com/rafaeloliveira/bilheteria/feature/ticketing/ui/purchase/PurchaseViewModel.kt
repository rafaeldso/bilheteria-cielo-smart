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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaeloliveira.bilheteria.core.common.AppError
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveAvailableEventsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveOpenPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.QuantityLimits
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.RecoverPendingPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.StartPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Route argument: the event being bought (plan §4: route arguments are only ids). */
private const val EVENT_ID_KEY = "eventId"

/** Quantity in editing, kept in the [SavedStateHandle] so it survives process death (BR-05 to BR-07). */
private const val QUANTITY_KEY = "quantity"

/** Reference of the purchase this screen started or found open; it is a random UUID, never card data. */
private const val PURCHASE_ID_KEY = "purchaseId"

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * SCR-02: quantity, payment, waiting, recovery and errors (BR-05 to BR-10, BR-14, BR-15, BR-25).
 *
 * The screen follows the local record: the open purchase decides `AwaitingPayment`/`Recovering`; when it settles the
 * state becomes `Completed` (a sale, to the receipt) or `Error` (`Failed`, with a new attempt allowed). On the first
 * read, an open purchase left from a previous run goes to the Cielo lookup before any new charge (BR-15).
 */
@HiltViewModel
internal class PurchaseViewModel
    @Inject
    constructor(
        private val observeAvailableEvents: ObserveAvailableEventsUseCase,
        private val observeOpenPurchase: ObserveOpenPurchaseUseCase,
        private val observePurchase: ObservePurchaseUseCase,
        private val startPurchase: StartPurchaseUseCase,
        private val recoverPending: RecoverPendingPurchaseUseCase,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val open = MutableStateFlow<OpenPurchase>(OpenPurchase.Unknown)
        private val outcome = MutableStateFlow<AttemptOutcome>(AttemptOutcome.None)
        private val selectedEvent = MutableStateFlow<AvailableEvent?>(null)
        private val quantity = savedStateHandle.getStateFlow(QUANTITY_KEY, QuantityLimits.MIN)
        private var startJob: Job? = null

        val uiState: StateFlow<PurchaseUiState> =
            combine(open, outcome, selectedEvent, quantity) { openPurchase, attempt, available, chosen ->
                PurchaseInputs(openPurchase, attempt, available, chosen).toUiState()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PurchaseUiState.Loading)

        init {
            collectEvent()
            collectOpenPurchase()
        }

        fun onEvent(event: PurchaseUiEvent) {
            when (event) {
                PurchaseUiEvent.OnIncrement -> changeQuantity(+1)
                PurchaseUiEvent.OnDecrement -> changeQuantity(-1)
                PurchaseUiEvent.OnPay -> if (currentState() is PurchaseUiState.Editing) startPayment()
                PurchaseUiEvent.OnRetry -> if (currentState() is PurchaseUiState.Error) startPayment()
            }
        }

        private fun currentState(): PurchaseUiState =
            PurchaseInputs(open.value, outcome.value, selectedEvent.value, quantity.value).toUiState()

        private fun changeQuantity(step: Int) {
            val state = currentState() as? PurchaseUiState.Editing ?: return
            val allowed = if (step > 0) state.canIncrement else state.canDecrement
            if (allowed) savedStateHandle[QUANTITY_KEY] = state.quantity + step
        }

        private fun collectEvent() {
            val eventId = savedStateHandle.get<String>(EVENT_ID_KEY).orEmpty()
            viewModelScope.launch {
                observeAvailableEvents().collect { result ->
                    if (result is DataResult.Success) {
                        selectedEvent.value =
                            result.value.firstOrNull { it.event.id == eventId }
                    }
                }
            }
        }

        private fun collectOpenPurchase() {
            viewModelScope.launch {
                var firstRead = true
                observeOpenPurchase().collect { result ->
                    when (result) {
                        is DataResult.Failure -> open.value = OpenPurchase.Unreadable
                        is DataResult.Success -> result.value?.let { present(it, firstRead) } ?: settle()
                    }
                    firstRead = false
                }
            }
        }

        private suspend fun present(
            purchase: Purchase,
            firstRead: Boolean,
        ) {
            savedStateHandle[PURCHASE_ID_KEY] = purchase.id
            open.value = OpenPurchase.Present(purchase)
            // The purchase was left open by an earlier run: ask Cielo before allowing any new charge (BR-15).
            // The result reaches the screen through the local record, so the return value is not needed here.
            if (firstRead) recoverPending()
        }

        /** No open purchase: read how the one this screen followed ended, then release the screen state. */
        private suspend fun settle() {
            val id = savedStateHandle.get<String>(PURCHASE_ID_KEY)
            if (id != null) {
                when (val result = observePurchase(id).first()) {
                    is DataResult.Success -> outcome.value = AttemptOutcome.Finished(result.value.rememberQuantity())
                    is DataResult.Failure -> outcome.value = AttemptOutcome.Failed(PurchaseErrorReason.Generic)
                }
            }
            open.value = OpenPurchase.None
        }

        /** A failed attempt is retried with the same quantity (BR-15), so the saved quantity follows the record. */
        private fun Purchase.rememberQuantity(): Purchase {
            savedStateHandle[QUANTITY_KEY] = quantity
            return this
        }

        private fun startPayment() {
            val selected = selectedEvent.value ?: return
            if (startJob?.isActive == true) return
            val chosen = clampQuantity(quantity.value, selected.available)
            outcome.value = AttemptOutcome.None
            startJob =
                viewModelScope.launch {
                    when (val result = startPurchase(selected, chosen)) {
                        is DataResult.Success -> savedStateHandle[PURCHASE_ID_KEY] = result.value.id
                        is DataResult.Failure -> outcome.value = result.error.toFailure()
                    }
                }
        }

        /** An already open purchase keeps the screen following it (BR-10); other failures are shown. */
        private fun AppError.toFailure(): AttemptOutcome =
            when (this) {
                TicketingError.PurchaseAlreadyOpen -> AttemptOutcome.None
                TicketingError.TerminalAppUnavailable -> AttemptOutcome.Failed(PurchaseErrorReason.NoCieloApp)
                else -> AttemptOutcome.Failed(PurchaseErrorReason.Generic)
            }
    }
