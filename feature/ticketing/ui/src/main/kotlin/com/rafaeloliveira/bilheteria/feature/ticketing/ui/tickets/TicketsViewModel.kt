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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.tickets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.TicketsRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSheet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.ShareBundle
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketPrinter
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.PrintTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ShareTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.SweepTicketFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** `TicketsRoute(purchaseId)` argument in the [SavedStateHandle], read from the route so it cannot drift. */
private val PURCHASE_ID_KEY: String = TicketsRoute.serializer().descriptor.getElementName(0)
private const val STOP_TIMEOUT_MS = 5_000L

/**
 * SCR-05 Ingressos (spec 002, BR-30 to BR-44). The tickets come from the purchase (Room is the source of truth), so a
 * late approval shows them with no extra work (BR-37). The print job lives in the singleton [TicketPrinter]: this class
 * only reads its status for this purchase and combines it with the share state it owns. One action at a time:
 * printing or sharing replaces the failure of the previous one, and sharing is ignored while printing (D3).
 */
@HiltViewModel
internal class TicketsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val observeTickets: ObserveTicketsUseCase,
        private val printTickets: PrintTicketsUseCase,
        private val shareTickets: ShareTicketsUseCase,
        private val printer: TicketPrinter,
        private val sweepFiles: SweepTicketFilesUseCase,
    ) : ViewModel() {
        // A tickets screen without its route argument is a programming error, not a screen state.
        private val purchaseId: String = checkNotNull(savedStateHandle.get<String>(PURCHASE_ID_KEY))
        private val attempt = MutableStateFlow(0)
        private val share = MutableStateFlow<ShareState>(ShareState.None)
        private val printRefused = MutableStateFlow(false)
        private var shareJob: Job? = null
        private var lastAction: LastAction = LastAction.None

        init {
            // "Ingressos impressos" is a notice of the moment: a leftover from an earlier visit must not come back.
            if (printer.status.value is PrintStatus.Done) printer.acknowledge()
            viewModelScope.launch { sweepFiles() } // images an earlier process left behind (plan §7)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<TicketsUiState> =
            combine(
                attempt.flatMapLatest { load() },
                printer.status,
                share,
                printRefused,
            ) { loaded, status, shareState, refused ->
                loaded.toUiState(status, shareState, refused, purchaseId)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TicketsUiState.Loading)

        fun onEvent(event: TicketsUiEvent) {
            when (event) {
                // "Tentar novamente" is only offered in Error (BR-36); the state is read from what the operator sees.
                TicketsUiEvent.OnRetry -> if (uiState.value == TicketsUiState.Error) attempt.update { it + 1 }

                TicketsUiEvent.OnPrint -> print()

                TicketsUiEvent.OnShare -> share()

                TicketsUiEvent.OnRetryAction -> retryLastAction()

                TicketsUiEvent.OnShareLaunched,
                TicketsUiEvent.OnShareLaunchFailed,
                TicketsUiEvent.OnActionDismissed,
                -> onNoticeEvent(event)
            }
        }

        /** Technical events that only consume or end a notice. */
        private fun onNoticeEvent(event: TicketsUiEvent) {
            when (event) {
                TicketsUiEvent.OnShareLaunched -> share.update { if (it is ShareState.Ready) ShareState.None else it }
                TicketsUiEvent.OnShareLaunchFailed -> share.value = ShareState.Failed
                TicketsUiEvent.OnActionDismissed -> printer.acknowledgeDone()
                else -> Unit
            }
        }

        private fun load(): Flow<Loaded> =
            observeTickets(purchaseId)
                .map { result ->
                    when (result) {
                        is DataResult.Failure -> Loaded.Failed
                        is DataResult.Success -> Loaded.Ready(result.value)
                    }
                }.onStart { emit(Loaded.Loading) }

        private fun print() {
            lastAction = LastAction.Print
            share.value = ShareState.None
            printRefused.value = false
            viewModelScope.launch {
                // Progress and a missing terminal come back through printer.status; any other refusal is shown
                // here (BR-40).
                val result = printTickets(purchaseId)
                if (result is DataResult.Failure && result.error != TicketingError.TerminalAppUnavailable) {
                    printRefused.value = true
                }
            }
        }

        private fun share() {
            if (shareJob?.isActive == true) return // a second tap while the images are being prepared is ignored
            lastAction = LastAction.Share
            printRefused.value = false
            printer.acknowledgeFailed() // sharing replaces an earlier print failure; a share failure is replaced below
            shareJob =
                viewModelScope.launch {
                    share.value =
                        when (val result = shareTickets(purchaseId)) {
                            is DataResult.Failure -> ShareState.Failed
                            is DataResult.Success -> result.value?.let(ShareState::Ready) ?: ShareState.None
                        }
                }
        }

        private fun retryLastAction() {
            when (lastAction) {
                LastAction.Print -> print()
                LastAction.Share -> share()
                LastAction.None -> Unit
            }
        }
    }

private sealed interface Loaded {
    data object Loading : Loaded

    data object Failed : Loaded

    data class Ready(
        val sheet: TicketSheet,
    ) : Loaded
}

private sealed interface ShareState {
    data object None : ShareState

    data object Failed : ShareState

    data class Ready(
        val bundle: ShareBundle,
    ) : ShareState
}

private enum class LastAction { None, Print, Share }

private fun TicketPrinter.acknowledgeDone() {
    if (status.value is PrintStatus.Done) acknowledge()
}

private fun TicketPrinter.acknowledgeFailed() {
    if (status.value is PrintStatus.Failed) acknowledge()
}

private fun Loaded.toUiState(
    status: PrintStatus,
    share: ShareState,
    printRefused: Boolean,
    purchaseId: String,
): TicketsUiState =
    when (this) {
        Loaded.Loading -> {
            TicketsUiState.Loading
        }

        Loaded.Failed -> {
            TicketsUiState.Error
        }

        is Loaded.Ready -> {
            TicketsUiState.Content(
                event = TicketEvent(sheet.set.eventName, sheet.set.eventStartsAt, sheet.set.venue),
                tickets = sheet.items.map { TicketItem(it.ticket.ordinal, it.ticket.total, it.qr) }.toImmutableList(),
                action = actionOf(status, share, printRefused, purchaseId, sheet.items.size),
            )
        }
    }

/** Print status of *this* purchase wins; otherwise the share state; a status of another purchase is just Idle. */
private fun actionOf(
    status: PrintStatus,
    share: ShareState,
    printRefused: Boolean,
    purchaseId: String,
    total: Int,
): TicketActionState =
    when {
        // A refusal of the latest request wins over an older print status (it would show an outdated reason).
        printRefused -> TicketActionState.PrintFailed(PrintFailureReason.Unknown, 0, total)

        else -> printActionOf(status, purchaseId) ?: shareActionOf(share)
    }

private fun printActionOf(
    status: PrintStatus,
    purchaseId: String,
): TicketActionState? =
    when {
        status is PrintStatus.Printing && status.purchaseId == purchaseId -> {
            TicketActionState.Printing(status.printed, status.total)
        }

        status is PrintStatus.Failed && status.purchaseId == purchaseId -> {
            TicketActionState.PrintFailed(status.reason, status.printed, status.total)
        }

        status is PrintStatus.Done && status.purchaseId == purchaseId -> {
            TicketActionState.Printed
        }

        else -> {
            null
        }
    }

private fun shareActionOf(share: ShareState): TicketActionState =
    when (share) {
        is ShareState.Ready -> TicketActionState.ShareReady(share.bundle)
        ShareState.Failed -> TicketActionState.ShareFailed
        ShareState.None -> TicketActionState.Idle
    }
