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
import kotlinx.coroutines.flow.map
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
 * SCR-02: quantity, payment, waiting, recovery and errors (BR-05 to BR-10, BR-14, BR-15, BR-25 to BR-27).
 *
 * The screen follows the local record: the open purchase decides `AwaitingPayment`/`Recovering`; when it settles the
 * state becomes `Completed` (a sale, to the receipt) or `Error` (`Failed`, with a new attempt allowed). An open
 * purchase left from a previous run goes to the Cielo lookup before any new charge (BR-15). While that lookup cannot be
 * completed, or the open purchase cannot be read, the state is `Error(RecoveryFailed)` and "Consultar de novo" repeats
 * the reading and the lookup: a payment is never started until the open purchase is known (BR-10, BR-26). The same
 * holds when the record of the attempt cannot be read after it settled: the outcome is unknown, so it is an
 * `Error(RecoveryFailed)` whose retry reads again (AC-25).
 *
 * Exception to AP-22 (plan §4): the open purchase and the catalog are collected from `init`, not through
 * `stateIn(WhileSubscribed)` alone, because the recovery of an open purchase (BR-15) must run even when no UI is
 * subscribed. Only [uiState] is shared with `WhileSubscribed`.
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
        private val selectedEvent = MutableStateFlow<EventLookup>(EventLookup.Unknown)
        private val recovery = MutableStateFlow(Recovery.Idle)
        private val quantity = savedStateHandle.getStateFlow(QUANTITY_KEY, QuantityLimits.MIN)
        private var openJob: Job? = null
        private var startJob: Job? = null
        private var recoveryJob: Job? = null

        /** True until the Cielo lookup of an open purchase from an earlier run was requested with success. */
        private var lookupPending = true

        val uiState: StateFlow<PurchaseUiState> =
            combine(open, outcome, selectedEvent, quantity, recovery, ::PurchaseInputs)
                .map { it.toUiState() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PurchaseUiState.Loading)

        init {
            collectEvent()
            collectOpenPurchase()
        }

        fun onEvent(event: PurchaseUiEvent) {
            when (event) {
                PurchaseUiEvent.OnIncrement -> changeQuantity(+1)
                PurchaseUiEvent.OnDecrement -> changeQuantity(-1)
                PurchaseUiEvent.OnPay -> if (currentState() is PurchaseUiState.Editing) startPayment()
                PurchaseUiEvent.OnRetry -> if (currentState() is PurchaseUiState.Error) retry()
            }
        }

        /**
         * The inputs are read directly (not through [uiState]) because [uiState] is only updated while it has a
         * collector, so its value can be stale when an event arrives.
         */
        private fun currentState(): PurchaseUiState =
            PurchaseInputs(open.value, outcome.value, selectedEvent.value, quantity.value, recovery.value).toUiState()

        private fun changeQuantity(step: Int) {
            val state = currentState() as? PurchaseUiState.Editing ?: return
            val allowed = if (step > 0) state.canIncrement else state.canDecrement
            if (allowed) savedStateHandle[QUANTITY_KEY] = state.quantity + step
        }

        private fun collectEvent() {
            val eventId = savedStateHandle.get<String>(EVENT_ID_KEY)
            if (eventId == null) {
                selectedEvent.value = EventLookup.Missing
                return
            }
            viewModelScope.launch {
                observeAvailableEvents().collect { result ->
                    selectedEvent.value =
                        when (result) {
                            is DataResult.Failure -> {
                                EventLookup.Missing
                            }

                            is DataResult.Success -> {
                                result.value
                                    .firstOrNull { it.event.id == eventId }
                                    ?.let(EventLookup::Found) ?: EventLookup.Missing
                            }
                        }
                }
            }
        }

        /** (Re)starts reading the open purchase; the repository flow ends after it emits a failure. */
        private fun collectOpenPurchase() {
            openJob?.cancel()
            open.value = OpenPurchase.Unknown
            recovery.value = Recovery.Idle
            openJob =
                viewModelScope.launch {
                    observeOpenPurchase().collect { result ->
                        when (result) {
                            is DataResult.Failure -> open.value = OpenPurchase.Unreadable
                            is DataResult.Success -> result.value?.let { present(it) } ?: settle()
                        }
                    }
                }
        }

        private suspend fun present(purchase: Purchase) {
            savedStateHandle[PURCHASE_ID_KEY] = purchase.id
            open.value = OpenPurchase.Present(purchase)
            // Left open by an earlier run: ask Cielo before allowing any new charge (BR-15). A failed lookup is only
            // repeated by the operator ("Consultar de novo"), never automatically.
            if (lookupPending && recovery.value == Recovery.Idle) lookUpPending()
        }

        /** The result reaches the screen through the local record, except for a failure (BR-26). */
        private suspend fun lookUpPending() {
            when (recoverPending()) {
                is DataResult.Success -> {
                    lookupPending = false
                    recovery.value = Recovery.Idle
                }

                is DataResult.Failure -> {
                    recovery.value = Recovery.Failed
                }
            }
        }

        /** No open purchase: read how the one this screen followed ended, then release the screen state. */
        private suspend fun settle() {
            lookupPending = false
            recovery.value = Recovery.Idle
            val id = savedStateHandle.get<String>(PURCHASE_ID_KEY)
            if (id != null) {
                when (val result = observePurchase(id).first()) {
                    is DataResult.Success -> {
                        // A failed attempt is retried with the same quantity (BR-15): the saved one follows the record.
                        savedStateHandle[QUANTITY_KEY] = result.value.quantity
                        outcome.value = AttemptOutcome.Finished(result.value)
                    }

                    is DataResult.Failure -> {
                        // How the attempt ended is unknown: it may have been charged, so never offer a new payment
                        // (BR-26). "Consultar de novo" reads the open purchase and the record again.
                        open.value = OpenPurchase.Unreadable
                        return
                    }
                }
            }
            open.value = OpenPurchase.None
        }

        /**
         * "Consultar de novo" repeats whatever is unknown before paying (BR-26): the reading of the open purchase, then
         * the Cielo lookup. Only an error that left no pending doubt starts a new payment.
         */
        private fun retry() {
            when {
                open.value is OpenPurchase.Unreadable -> collectOpenPurchase()
                recovery.value == Recovery.Failed -> retryLookup()
                else -> startPayment()
            }
        }

        /** The error stays on screen until the lookup answers: success follows the record, failure keeps the error. */
        private fun retryLookup() {
            if (recoveryJob?.isActive == true) return
            recoveryJob = viewModelScope.launch { lookUpPending() }
        }

        private fun startPayment() {
            val selected = (selectedEvent.value as? EventLookup.Found)?.event ?: return
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
