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
import com.rafaeloliveira.bilheteria.core.common.DefaultDispatcher
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSheet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketWithQr
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observes the tickets of a purchase with their QR Codes (BR-30, BR-36, BR-37). The purchase is the single source of
 * truth, so a late approval (BR-37 of this spec, BR-29 of the 001) turns the failure into tickets with no extra work.
 *
 * Failures: the purchase cannot be read or does not exist → the repository error; it exists but is not approved →
 * [TicketingError.TicketsUnavailable] (BR-33, BR-36); a QR Code cannot be encoded → the encoder error, with no ticket
 * shown at all (BR-36).
 */
class ObserveTicketsUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
        private val generateTickets: GenerateTicketsUseCase,
        private val qrEncoder: QrEncoder,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    ) {
        operator fun invoke(purchaseId: String): Flow<DataResult<TicketSheet>> =
            repository
                .observePurchase(purchaseId)
                .map { result ->
                    when (result) {
                        is DataResult.Failure -> result
                        is DataResult.Success -> sheetOf(result.value)
                    }
                }.flowOn(dispatcher)

        private fun sheetOf(
            purchase: com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase,
        ): DataResult<TicketSheet> {
            val set = generateTickets(purchase) ?: return DataResult.Failure(TicketingError.TicketsUnavailable)
            val items =
                set.tickets.map { ticket ->
                    when (val encoded = qrEncoder.encode(ticket.code)) {
                        is DataResult.Failure -> return encoded
                        is DataResult.Success -> TicketWithQr(ticket, encoded.value)
                    }
                }
            return DataResult.Success(TicketSheet(set, items))
        }
    }
