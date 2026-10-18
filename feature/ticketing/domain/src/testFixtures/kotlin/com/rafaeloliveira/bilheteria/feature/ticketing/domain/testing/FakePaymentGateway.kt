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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.ReferenceGenerator

/** Records what was sent to the terminal; set [terminalAvailable] = false for AC-12. */
class FakePaymentGateway : PaymentGateway {
    var terminalAvailable: Boolean = true
    val paymentRequests = mutableListOf<Purchase>()
    val lookupRequests = mutableListOf<Int>()
    var finished: Int = 0
        private set

    override suspend fun requestPayment(purchase: Purchase): DataResult<Unit> {
        if (!terminalAvailable) return DataResult.Failure(TicketingError.TerminalAppUnavailable)
        paymentRequests += purchase
        return DataResult.Success(Unit)
    }

    override suspend fun requestLookup(page: Int): DataResult<Unit> {
        if (!terminalAvailable) return DataResult.Failure(TicketingError.TerminalAppUnavailable)
        lookupRequests += page
        return DataResult.Success(Unit)
    }

    override fun finish() {
        finished++
    }
}

/** Returns the given references in order. */
class FixedReferenceGenerator(
    vararg references: String,
) : ReferenceGenerator {
    private val queue = ArrayDeque(references.toList())

    override fun next(): PurchaseReference = PurchaseReference(queue.removeFirst())
}
