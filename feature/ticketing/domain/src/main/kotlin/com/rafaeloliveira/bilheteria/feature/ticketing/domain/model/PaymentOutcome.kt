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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.model

/**
 * A Cielo response already interpreted by the data layer.
 *
 * Mapping of Cielo error codes (spec Q8, option A, approved 2026-10-17):
 * - code 1, "cancelado pelo usuário" → [Cancelled]: someone cancelled on the terminal (BR-12).
 * - code 3, "erro no pagamento" → [Declined] (BR-13). Cielo has no specific "declined by the issuer"
 *   code; code 3 is the only one about the payment itself.
 * - code 2 (generic), code 4 (authentication) and unknown codes → [Failed] (BR-14). They point to a problem on the app
 *   side (configuration, credentials), not to the card: calling them "declined" would make the operator ask for
 *   another card when no card would work.
 * - Unreadable response → [Unreadable]: the payment may have been approved, so it is never treated as approved or
 *   failed; the purchase goes to recovery and Cielo is queried by reference before a new charge (BR-25).
 *
 * Validated in the Cielo emulator (research.md §13): its "Erro" button returns code 2, so [Declined] is covered by
 * unit tests only.
 */
sealed interface PaymentOutcome {
    /** Reference echoed by Cielo, when present. Checked against the open purchase before applying (anti-spoofing). */
    val reference: PurchaseReference?

    /** `amount` comes from `payments[].amount`: `paidAmount` is 0 in approved responses (research.md §13). */
    data class Approved(
        override val reference: PurchaseReference?,
        val amount: Money,
        val transaction: TransactionData,
    ) : PaymentOutcome

    data class Declined(
        override val reference: PurchaseReference?,
    ) : PaymentOutcome

    data class Cancelled(
        override val reference: PurchaseReference?,
    ) : PaymentOutcome

    data class Failed(
        override val reference: PurchaseReference?,
        val reason: FailureReason,
    ) : PaymentOutcome

    data object Unreadable : PaymentOutcome {
        override val reference: PurchaseReference? = null
    }

    /** One page of the `lio://orders` listing, already filtered to orders carrying a reference. */
    data class LookupPage(
        val page: Int,
        val totalPages: Int,
        val orders: List<LookupOrder>,
    ) : PaymentOutcome {
        override val reference: PurchaseReference? = null
    }
}

/** An order found in the listing: [approved] is set only when a payment with status 1 (or 0, Pix) exists (§13). */
data class LookupOrder(
    val reference: PurchaseReference,
    val approved: PaymentOutcome.Approved?,
)
