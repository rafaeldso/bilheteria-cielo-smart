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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import kotlinx.coroutines.flow.StateFlow

/** Encodes a ticket into a QR Code matrix (BR-30). CPU work: the caller picks the dispatcher. */
fun interface QrEncoder {
    /** Fails with `TicketingError.TicketGenerationFailed` when the content cannot be encoded. */
    fun encode(code: TicketCode): DataResult<QrMatrix>
}

/** Port to the terminal printer, over `lio://print` (BR-38 to BR-40, BR-44). */
interface TicketPrinter {
    val status: StateFlow<PrintStatus>

    /**
     * Prints the tickets of [set] one at a time, starting at the ticket with index [from] (0-based; the first one not
     * printed yet when resuming). Returns when the job was handed over; progress and the result come in [status].
     * Fails with `TicketingError.TerminalAppUnavailable` when nothing was sent.
     */
    suspend fun print(
        set: TicketSet,
        from: Int = 0,
    ): DataResult<Unit>

    /** Leaves `Failed` or `Done` and goes back to `Idle`. */
    fun acknowledge()
}

/** Image files the system share sheet can read, as `content://` URIs (BR-41, BR-42). */
data class ShareBundle(
    val uris: List<String>,
)

/** Prepares the ticket images for sharing. Fails with `TicketingError.TicketGenerationFailed`. */
interface TicketFiles {
    suspend fun prepareShare(set: TicketSet): DataResult<ShareBundle>

    /** Deletes the ticket images left behind by an earlier process (shared and print images). Never throws. */
    suspend fun sweepStale()
}
