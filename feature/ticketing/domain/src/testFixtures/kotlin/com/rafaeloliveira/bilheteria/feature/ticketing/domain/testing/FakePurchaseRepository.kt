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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory purchases honoring the real contract: at most one open purchase (BR-10), history with sales only,
 * newest first (BR-18). Set [failStorage] to simulate `StorageFailure`.
 */
class FakePurchaseRepository(
    initial: List<Purchase> = emptyList(),
) : PurchaseRepository {
    private val purchases = MutableStateFlow(initial)
    var failStorage: Boolean = false

    val all: List<Purchase> get() = purchases.value

    override fun observePurchase(id: String): Flow<DataResult<Purchase>> =
        purchases.map { list ->
            list.firstOrNull { it.id == id }?.let { DataResult.Success(it) }
                ?: DataResult.Failure(TicketingError.StorageFailure)
        }

    override fun observeHistory(): Flow<DataResult<List<Purchase>>> =
        purchases.map { list ->
            if (failStorage) {
                DataResult.Failure(TicketingError.StorageFailure)
            } else {
                DataResult.Success(list.filter { it.status.isSale }.sortedByDescending { it.createdAt })
            }
        }

    override fun observeOpenPurchase(): Flow<DataResult<Purchase?>> =
        purchases.map { list -> DataResult.Success(list.firstOrNull { it.status.isOpen }) }

    override fun observeApprovedQuantities(): Flow<DataResult<Map<String, Int>>> =
        purchases.map { list ->
            DataResult.Success(
                list
                    .filter { it.status == PurchaseStatus.Approved }
                    .groupBy { it.eventId }
                    .mapValues { (_, items) -> items.sumOf { it.quantity } },
            )
        }

    override suspend fun createPending(purchase: Purchase): DataResult<Unit> =
        when {
            failStorage -> DataResult.Failure(TicketingError.StorageFailure)
            purchases.value.any { it.status.isOpen } -> DataResult.Failure(TicketingError.PurchaseAlreadyOpen)
            else -> DataResult.Success(Unit).also { purchases.value = purchases.value + purchase }
        }

    override suspend fun update(purchase: Purchase): DataResult<Unit> =
        if (failStorage) {
            DataResult.Failure(TicketingError.StorageFailure)
        } else {
            DataResult.Success(Unit).also {
                purchases.value = purchases.value.map { if (it.id == purchase.id) purchase else it }
            }
        }

    override suspend fun findOpen(): DataResult<Purchase?> =
        if (failStorage) {
            DataResult.Failure(TicketingError.StorageFailure)
        } else {
            DataResult.Success(purchases.value.firstOrNull { it.status.isOpen })
        }
}
