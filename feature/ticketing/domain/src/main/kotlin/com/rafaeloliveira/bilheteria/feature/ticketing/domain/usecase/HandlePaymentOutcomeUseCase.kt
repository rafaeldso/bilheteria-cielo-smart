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
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Applies a Cielo response to the open purchase (BR-11 to BR-15, BR-25). The response is untrusted input (STRIDE,
 * spoofing): it is applied only when there is an open purchase, its `reference` (when present) matches and, for an
 * approval, the paid amount equals the purchase total. Anything else is discarded with a `callback_discarded` log
 * that carries the reason only, never the payment data, and is not an operator error (returns `Success`).
 *
 * Mapping: Approved (with the transaction), Cancelled, Declined and Failed(reason) become the purchase final status
 * and end the payment session (`gateway.finish()`). `Unreadable` never becomes Approved or Failed: it starts the
 * recovery (BR-25).
 *
 * The whole handling runs under the [PurchaseWriteLock], shared with the recovery, so snapshot-based updates do
 * not overwrite each other.
 *
 * Late approval (BR-29): an `Approved` response whose `reference` is not the open purchase's (or with no open purchase)
 * is applied to the purchase it names when that purchase is `Failed`/`NotFound` (the lookup found nothing, then the
 * terminal finished the payment) and the paid amount equals its total. Only the repository is updated: the open
 * purchase and the gateway session belong to another payment and stay untouched.
 *
 * Lookup algorithm (BR-15), applied only to a purchase in [PurchaseStatus.Recovering]. `LookupPage.page` is the
 * 0-based index that was requested (the data layer normalizes Cielo's numbering). Cielo lists orders from the oldest
 * to the newest, and the purchase is among the most recent, so after page 0 the search goes from the last page
 * backwards:
 * 1. Order with the purchase `reference` and an approved payment of the purchase total: Approved with its transaction.
 * 2. Order without an approved payment (or with another amount): Failed/NotFound. Nothing was charged for it, so a
 *    new attempt is safe.
 * 3. Not found: after page 0 request `totalPages - 1`; after any other page request `page - 1`. Stop with
 *    Failed/NotFound once the 4 last pages were read (`page <= totalPages - 4`) or when the next page would be 0,
 *    which was already read. With `totalPages <= 1` the first page is the whole listing.
 */
class HandlePaymentOutcomeUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
        private val gateway: PaymentGateway,
        private val recover: RecoverPendingPurchaseUseCase,
        private val clock: Clock,
        private val logger: Logger,
        private val lock: PurchaseWriteLock,
    ) {
        suspend operator fun invoke(outcome: PaymentOutcome): DataResult<Unit> = lock.withLock { handle(outcome) }

        private suspend fun handle(outcome: PaymentOutcome): DataResult<Unit> =
            when (val open = repository.findOpen()) {
                is DataResult.Failure -> open
                is DataResult.Success -> handleOpen(open.value, outcome)
            }

        private suspend fun handleOpen(
            purchase: Purchase?,
            outcome: PaymentOutcome,
        ): DataResult<Unit> =
            when {
                purchase == null -> {
                    approveLate(outcome, NO_OPEN_PURCHASE)
                }

                outcome.reference != null && outcome.reference != purchase.reference -> {
                    approveLate(outcome, REFERENCE_MISMATCH)
                }

                else -> {
                    apply(purchase, outcome)
                }
            }

        private suspend fun approveLate(
            outcome: PaymentOutcome,
            discardReason: String,
        ): DataResult<Unit> {
            val approved = outcome as? PaymentOutcome.Approved
            val reference = approved?.reference ?: return discard(discardReason)
            return when (val found = repository.findById(reference.value)) {
                is DataResult.Failure -> {
                    found
                }

                is DataResult.Success -> {
                    val failed = found.value?.takeIf { it.isLateApprovable(approved) }
                    if (failed == null) discard(discardReason) else saveLateApproval(failed, approved)
                }
            }
        }

        private fun Purchase.isLateApprovable(approved: PaymentOutcome.Approved): Boolean =
            status == PurchaseStatus.Failed &&
                failureReason == FailureReason.NotFound &&
                approved.amount == total

        private suspend fun saveLateApproval(
            purchase: Purchase,
            approved: PaymentOutcome.Approved,
        ): DataResult<Unit> {
            val saved =
                repository.update(
                    purchase.copy(
                        status = PurchaseStatus.Approved,
                        failureReason = null,
                        transaction = approved.transaction,
                        updatedAt = clock.instant(),
                    ),
                )
            if (saved is DataResult.Success) {
                logger.info("late_approval reference=${purchase.reference.value}")
            }
            return saved
        }

        private suspend fun apply(
            purchase: Purchase,
            outcome: PaymentOutcome,
        ): DataResult<Unit> =
            when (outcome) {
                is PaymentOutcome.Approved -> approve(purchase, outcome)
                is PaymentOutcome.Cancelled -> conclude(purchase, PurchaseStatus.Cancelled)
                is PaymentOutcome.Declined -> conclude(purchase, PurchaseStatus.Declined)
                is PaymentOutcome.Failed -> conclude(purchase, PurchaseStatus.Failed, outcome.reason)
                PaymentOutcome.Unreadable -> recoverPurchase()
                is PaymentOutcome.LookupPage -> lookup(purchase, outcome)
            }

        private suspend fun recoverPurchase(): DataResult<Unit> =
            when (val recovery = recover.invokeLocked()) {
                is DataResult.Failure -> recovery
                is DataResult.Success -> DataResult.Success(Unit)
            }

        private suspend fun approve(
            purchase: Purchase,
            outcome: PaymentOutcome.Approved,
        ): DataResult<Unit> =
            if (outcome.amount == purchase.total) {
                conclude(purchase, PurchaseStatus.Approved, transaction = outcome.transaction)
            } else {
                discard(AMOUNT_MISMATCH)
            }

        private suspend fun lookup(
            purchase: Purchase,
            page: PaymentOutcome.LookupPage,
        ): DataResult<Unit> {
            if (purchase.status != PurchaseStatus.Recovering) return discard(NOT_RECOVERING)
            val order = page.orders.firstOrNull { it.reference == purchase.reference }
            logger.info("lookup_page page=${page.page} found=${order != null}")
            val approved = order?.approved
            return when {
                order == null -> {
                    searchNextPage(purchase, page)
                }

                approved != null && approved.amount == purchase.total -> {
                    conclude(purchase, PurchaseStatus.Approved, transaction = approved.transaction)
                }

                else -> {
                    notFound(purchase)
                }
            }
        }

        private suspend fun searchNextPage(
            purchase: Purchase,
            page: PaymentOutcome.LookupPage,
        ): DataResult<Unit> {
            val next = nextPage(page.page, page.totalPages) ?: return notFound(purchase)
            return gateway.requestLookup(next)
        }

        /** Next 0-based page to request, or null when the search ends (see the class KDoc). */
        private fun nextPage(
            page: Int,
            totalPages: Int,
        ): Int? {
            val next =
                when {
                    page !in 0 until totalPages -> null
                    page == FIRST_PAGE -> totalPages - 1
                    page <= totalPages - LAST_PAGES_WINDOW -> null
                    else -> page - 1
                }
            return next?.takeIf { it > FIRST_PAGE }
        }

        private suspend fun notFound(purchase: Purchase): DataResult<Unit> =
            conclude(purchase, PurchaseStatus.Failed, FailureReason.NotFound)

        private suspend fun conclude(
            purchase: Purchase,
            status: PurchaseStatus,
            reason: FailureReason? = null,
            transaction: TransactionData? = null,
        ): DataResult<Unit> {
            val updated =
                purchase.copy(
                    status = status,
                    failureReason = reason,
                    transaction = transaction,
                    updatedAt = clock.instant(),
                )
            val saved = repository.update(updated)
            if (saved is DataResult.Success) {
                logger.info("payment_outcome status=$status reference=${purchase.reference.value}")
                gateway.finish()
            }
            return saved
        }

        private fun discard(reason: String): DataResult<Unit> {
            logger.warn("callback_discarded reason=$reason")
            return DataResult.Success(Unit)
        }

        private companion object {
            const val FIRST_PAGE = 0
            const val LAST_PAGES_WINDOW = 4
            const val NO_OPEN_PURCHASE = "no_open_purchase"
            const val REFERENCE_MISMATCH = "reference_mismatch"
            const val AMOUNT_MISMATCH = "amount_mismatch"
            const val NOT_RECOVERING = "not_recovering"
        }
    }
