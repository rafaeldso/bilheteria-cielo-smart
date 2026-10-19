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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import javax.inject.Inject

/**
 * Reads a purchase once and derives its tickets, for the one-shot actions (print, share). A purchase that cannot be
 * read gives the repository error; one that does not exist or is not approved gives
 * [TicketingError.TicketsUnavailable] (BR-33, BR-36).
 */
class LoadTicketSetUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
        private val generateTickets: GenerateTicketsUseCase,
    ) {
        suspend operator fun invoke(purchaseId: String): DataResult<TicketSet> =
            when (val found = repository.findById(purchaseId)) {
                is DataResult.Failure -> {
                    found
                }

                is DataResult.Success -> {
                    found.value
                        ?.let(generateTickets::invoke)
                        ?.let { DataResult.Success(it) }
                        ?: DataResult.Failure(TicketingError.TicketsUnavailable)
                }
            }
    }
