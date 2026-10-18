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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Starts the recovery of the open purchase (BR-15, BR-25): marks it [PurchaseStatus.Recovering] and asks Cielo for the
 * first page of the order listing. The listing arrives later through the callback, handled by
 * `HandlePaymentOutcomeUseCase`, which keeps paging until it finds the purchase `reference` or gives up.
 *
 * Returns `Success(true)` when a lookup was requested and `Success(false)` when there is no open purchase. Without the
 * Cielo app nothing could have been charged, so the purchase ends as `Failed`/`TerminalAppUnavailable` and the
 * failure is returned.
 */
class RecoverPendingPurchaseUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
        private val gateway: PaymentGateway,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): DataResult<Boolean> =
            when (val open = repository.findOpen()) {
                is DataResult.Failure -> open
                is DataResult.Success -> open.value?.let { recover(it) } ?: DataResult.Success(false)
            }

        private suspend fun recover(purchase: Purchase): DataResult<Boolean> {
            val recovering = purchase.copy(status = PurchaseStatus.Recovering, updatedAt = clock.instant())
            val saved = repository.update(recovering)
            if (saved is DataResult.Failure) return saved
            val lookup = gateway.requestLookup(page = FIRST_PAGE)
            if (lookup is DataResult.Failure) return abandon(recovering, lookup)
            return DataResult.Success(true)
        }

        private suspend fun abandon(
            purchase: Purchase,
            lookup: DataResult.Failure,
        ): DataResult<Boolean> {
            if (lookup.error == TicketingError.TerminalAppUnavailable) {
                val failed =
                    purchase.copy(
                        status = PurchaseStatus.Failed,
                        failureReason = FailureReason.TerminalAppUnavailable,
                        updatedAt = clock.instant(),
                    )
                if (repository.update(failed) is DataResult.Success) gateway.finish()
            }
            return lookup
        }

        private companion object {
            const val FIRST_PAGE = 0
        }
    }
