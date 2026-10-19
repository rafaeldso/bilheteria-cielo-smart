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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import javax.inject.Inject

/**
 * Derives the tickets of an approved purchase (BR-30 to BR-33): one ticket per purchased unit, linked to the purchase
 * by its `reference`. Nothing is stored (BR-32): the same purchase always yields the same codes. The code carries only
 * `reference` and order (BR-31), never transaction data, card data or credentials.
 */
class GenerateTicketsUseCase
    @Inject
    constructor() {
        /** The tickets of [purchase], or null when it is not [PurchaseStatus.Approved] (BR-33). */
        operator fun invoke(purchase: Purchase): TicketSet? {
            if (purchase.status != PurchaseStatus.Approved) return null
            val total = purchase.quantity
            return TicketSet(
                purchaseId = purchase.id,
                eventName = purchase.eventName,
                eventStartsAt = purchase.eventStartsAt,
                venue = purchase.venue,
                tickets = (1..total).map { ordinal -> Ticket(ordinal, total, codeOf(purchase, ordinal)) },
            )
        }

        private fun codeOf(
            purchase: Purchase,
            ordinal: Int,
        ) = TicketCode("$CODE_PREFIX${purchase.reference.value}:$ordinal")

        internal companion object {
            /** Format version 1 of the QR content (BR-31). */
            const val CODE_PREFIX = "bilheteria:1:"
        }
    }
