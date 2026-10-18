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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference

/**
 * Port to the payment terminal (ADR-0010). Requests are fire-and-forget: Cielo answers later through the
 * callback, which calls `HandlePaymentOutcomeUseCase`.
 */
interface PaymentGateway {
    /** Opens the payment for [purchase]. Fails with `TicketingError.TerminalAppUnavailable` (AC-12). */
    suspend fun requestPayment(purchase: Purchase): DataResult<Unit>

    /** Requests one page of the order listing, used by recovery (BR-15, BR-25). */
    suspend fun requestLookup(page: Int): DataResult<Unit>

    /** Ends the payment session (stops the foreground service). */
    fun finish()
}

/** Generates purchase references; injected so tests are deterministic. */
fun interface ReferenceGenerator {
    fun next(): PurchaseReference
}
