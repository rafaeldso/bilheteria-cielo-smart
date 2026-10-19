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

import androidx.compose.runtime.Immutable
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.ShareBundle
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant

/** States of SCR-05 Ingressos (spec 002, section 6). */
@Immutable
internal sealed interface TicketsUiState {
    /** Reading the purchase and encoding the QR Codes (AC-34). */
    data object Loading : TicketsUiState

    /** One QR Code per ticket of an approved purchase (BR-30, BR-35), and the state of print and share ([action]). */
    data class Content(
        val event: TicketEvent,
        val tickets: ImmutableList<TicketItem>,
        val action: TicketActionState,
    ) : TicketsUiState

    /** The purchase cannot be read, is not approved, does not exist, or a QR Code could not be encoded (BR-36). */
    data object Error : TicketsUiState
}

/** Event data shown above the tickets (BR-35). Raw values: the screen formats them. */
@Immutable
internal data class TicketEvent(
    val name: String,
    val startsAt: Instant,
    val venue: String,
)

/** One ticket: its order, the total and the QR Code matrix; the screen draws it with `DsQrCode`. */
@Immutable
internal data class TicketItem(
    val ordinal: Int,
    val total: Int,
    val qr: QrMatrix,
)

/** State of the print and share actions inside [TicketsUiState.Content] (spec 002, section 6). */
@Immutable
internal sealed interface TicketActionState {
    /** Both actions available. */
    data object Idle : TicketActionState

    /** [printed] of [total] confirmed; printing is busy and sharing is unavailable (BR-39, D3). */
    data class Printing(
        val printed: Int,
        val total: Int,
    ) : TicketActionState

    /** The print job stopped after [printed] of [total]; a retry resumes there (BR-40). */
    data class PrintFailed(
        val reason: PrintFailureReason,
        val printed: Int,
        val total: Int,
    ) : TicketActionState

    /** Cielo confirmed every ticket (BR-44). */
    data object Printed : TicketActionState

    /** The images could not be prepared or no app can receive them (BR-43). */
    data object ShareFailed : TicketActionState

    /** Images ready: the Route opens the system share sheet and answers with `OnShareLaunched`. */
    data class ShareReady(
        val bundle: ShareBundle,
    ) : TicketActionState
}
