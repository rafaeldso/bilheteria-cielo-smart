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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import kotlinx.coroutines.flow.Flow

/** Purchases stored locally; the single source of truth of the payment flow (ADR-0010). */
interface PurchaseRepository {
    fun observePurchase(id: String): Flow<DataResult<Purchase>>

    /** Sales only (Approved, Declined, Cancelled), newest first (BR-18). */
    fun observeHistory(): Flow<DataResult<List<Purchase>>>

    /** The open purchase (Pending or Recovering), if any. There is never more than one (BR-10). */
    fun observeOpenPurchase(): Flow<DataResult<Purchase?>>

    /** Approved ticket count per event id (BR-17). */
    fun observeApprovedQuantities(): Flow<DataResult<Map<String, Int>>>

    /** Fails with `TicketingError.PurchaseAlreadyOpen` when another purchase is open (BR-10). */
    suspend fun createPending(purchase: Purchase): DataResult<Unit>

    suspend fun update(purchase: Purchase): DataResult<Unit>

    suspend fun findOpen(): DataResult<Purchase?>

    /** A purchase by id (= its `reference`), open or not; `Success(null)` when there is none. Used by BR-29. */
    suspend fun findById(id: String): DataResult<Purchase?>
}
