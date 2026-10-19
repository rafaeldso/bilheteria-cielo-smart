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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketPrinter
import javax.inject.Inject

/**
 * Prints the tickets of an approved purchase (BR-38). While a job is running the request is ignored (BR-39, AC-39).
 * After a failure for the **same** purchase it resumes from the first ticket not printed yet, so a retry never
 * reprints what already came out (BR-40, AC-40). It does not start while a purchase is open (a payment answer could be
 * mistaken for a print answer, plan R2). Printing does not touch the purchase or the QR Codes.
 */
class PrintTicketsUseCase
    @Inject
    constructor(
        private val loadTicketSet: LoadTicketSetUseCase,
        private val repository: PurchaseRepository,
        private val printer: TicketPrinter,
    ) {
        suspend operator fun invoke(purchaseId: String): DataResult<Unit> {
            val status = printer.status.value
            if (status is PrintStatus.Printing) return DataResult.Success(Unit)
            openPurchaseFailure()?.let { return it }
            return when (val loaded = loadTicketSet(purchaseId)) {
                is DataResult.Failure -> {
                    loaded
                }

                is DataResult.Success -> {
                    val set = loaded.value
                    val resumeFrom =
                        (status as? PrintStatus.Failed)?.takeIf { it.purchaseId == purchaseId }?.printed ?: 0
                    printer.print(set, resumeFrom.coerceIn(0, set.tickets.size - 1))
                }
            }
        }

        /** A print answer and a payment error share the same shape: never print while a payment may answer (R2). */
        private suspend fun openPurchaseFailure(): DataResult.Failure? =
            when (val open = repository.findOpen()) {
                is DataResult.Failure -> {
                    open
                }

                is DataResult.Success -> {
                    DataResult.Failure(TicketingError.PurchaseAlreadyOpen).takeIf {
                        open.value !=
                            null
                    }
                }
            }
    }
