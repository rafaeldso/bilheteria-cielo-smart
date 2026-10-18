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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.ReferenceGenerator
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Starts the payment of a purchase.
 *
 * The purchase is stored as `Pending` before the terminal is opened (BR-08); if it cannot be stored, including
 * when another purchase is already open (BR-10), the gateway is never called. Then the payment is requested
 * without fixing the payment method (BR-09). When the payment cannot be started the purchase ends as `Failed`,
 * allowing a new attempt (BR-14).
 */
class StartPurchaseUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
        private val gateway: PaymentGateway,
        private val references: ReferenceGenerator,
        private val clock: Clock,
    ) {
        /** Requires [quantity] within the BR-07 limits (1 to 10, never above the available tickets). */
        suspend operator fun invoke(
            event: AvailableEvent,
            quantity: Int,
        ): DataResult<Purchase> {
            val max = QuantityLimits.max(event.available)
            require(quantity in QuantityLimits.MIN..max) { "Quantity must be within ${QuantityLimits.MIN}..$max" }
            val purchase = pendingPurchase(event, quantity)
            val stored = repository.createPending(purchase)
            if (stored is DataResult.Failure) return stored
            return when (val requested = gateway.requestPayment(purchase)) {
                is DataResult.Success -> DataResult.Success(purchase)
                is DataResult.Failure -> failPurchase(purchase, requested)
            }
        }

        private fun pendingPurchase(
            event: AvailableEvent,
            quantity: Int,
        ): Purchase {
            val reference = references.next()
            val now = clock.instant()
            return Purchase(
                id = reference.value,
                reference = reference,
                eventId = event.event.id,
                eventName = event.event.name,
                eventStartsAt = event.event.startsAt,
                venue = event.event.venue,
                unitPrice = event.event.unitPrice,
                quantity = quantity,
                total = event.event.unitPrice * quantity,
                status = PurchaseStatus.Pending,
                failureReason = null,
                transaction = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        /** Marks the purchase as `Failed` (BR-14) and returns the gateway failure, even if the update fails. */
        private suspend fun failPurchase(
            purchase: Purchase,
            failure: DataResult.Failure,
        ): DataResult.Failure {
            val reason =
                if (failure.error == TicketingError.TerminalAppUnavailable) {
                    FailureReason.TerminalAppUnavailable
                } else {
                    FailureReason.Generic
                }
            repository.update(
                purchase.copy(status = PurchaseStatus.Failed, failureReason = reason, updatedAt = clock.instant()),
            )
            return failure
        }
    }
