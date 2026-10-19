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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.ShareBundle
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketFiles
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Deterministic 1x1 matrix per code; set [fail] for the generation error (AC-35). */
class FakeQrEncoder : QrEncoder {
    var fail: Boolean = false
    val encoded = mutableListOf<TicketCode>()

    override fun encode(code: TicketCode): DataResult<QrMatrix> {
        if (fail) return DataResult.Failure(TicketingError.TicketGenerationFailed)
        encoded += code
        return DataResult.Success(QrMatrix(side = 1, modules = booleanArrayOf(code.value.length % 2 == 0)))
    }
}

/** Records print requests; drive [status] from the test to simulate Cielo's answers. */
class FakeTicketPrinter : TicketPrinter {
    private val state = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    override val status: StateFlow<PrintStatus> = state

    var terminalAvailable: Boolean = true
    val requests = mutableListOf<Pair<TicketSet, Int>>()
    var acknowledged: Int = 0
        private set

    fun emit(status: PrintStatus) {
        state.value = status
    }

    override suspend fun print(
        set: TicketSet,
        from: Int,
    ): DataResult<Unit> {
        if (!terminalAvailable) {
            // Like the real printer: nothing was sent and the job is Failed, so the screen has something to show.
            state.value =
                PrintStatus.Failed(set.purchaseId, PrintFailureReason.TerminalAppUnavailable, from, set.tickets.size)
            return DataResult.Failure(TicketingError.TerminalAppUnavailable)
        }
        requests += set to from
        state.value = PrintStatus.Printing(purchaseId = set.purchaseId, printed = from, total = set.tickets.size)
        return DataResult.Success(Unit)
    }

    override fun acknowledge() {
        acknowledged++
        state.value = PrintStatus.Idle
    }
}

/** Returns one fake URI per ticket; set [fail] for AC-43. */
class FakeTicketFiles : TicketFiles {
    var fail: Boolean = false
    val prepared = mutableListOf<TicketSet>()
    var swept: Int = 0
        private set

    override suspend fun prepareShare(set: TicketSet): DataResult<ShareBundle> {
        if (fail) return DataResult.Failure(TicketingError.TicketGenerationFailed)
        prepared += set
        return DataResult.Success(ShareBundle(set.tickets.map { "content://test/tickets/${it.ordinal}.png" }))
    }

    override suspend fun sweepStale() {
        swept++
    }
}
