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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase

import android.database.sqlite.SQLiteException
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.common.IoDispatcher
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.core.database.PurchaseDao
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Room-backed [PurchaseRepository] (ADR-0010: Room is the single source of truth of the payment flow).
 *
 * Error matrix (plan §5): any `SQLiteException`, and any row the domain rejects, becomes
 * `TicketingError.StorageFailure`; technical exceptions never cross this class. A purchase id that does not
 * exist also reports `StorageFailure` in [observePurchase], because the contract has no "not found" error.
 */
internal class DefaultPurchaseRepository
    @Inject
    constructor(
        private val dao: PurchaseDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val logger: Logger,
    ) : PurchaseRepository {
        override fun observePurchase(id: String): Flow<DataResult<Purchase>> =
            dao
                .observeById(id)
                .map { entity ->
                    if (entity == null) {
                        DataResult.Failure(TicketingError.StorageFailure)
                    } else {
                        guarded("observe_purchase") { entity.toDomain() }
                    }
                }.recoverStorage("observe_purchase")

        override fun observeHistory(): Flow<DataResult<List<Purchase>>> =
            dao
                .observeByStatuses(SALE_STATUSES)
                .map { rows -> guarded("observe_history") { rows.map { it.toDomain() } } }
                .recoverStorage("observe_history")

        override fun observeOpenPurchase(): Flow<DataResult<Purchase?>> =
            dao
                .observeOpen(OPEN_STATUSES)
                .map { row -> guarded("observe_open") { row?.toDomain() } }
                .recoverStorage("observe_open")

        override fun observeApprovedQuantities(): Flow<DataResult<Map<String, Int>>> =
            dao
                .observeQuantitiesByStatus(PurchaseStatus.Approved.name)
                .map { rows -> DataResult.Success(rows.associate { it.eventId to it.quantity }) }
                .recoverStorage("observe_approved_quantities")

        override suspend fun createPending(purchase: Purchase): DataResult<Unit> =
            withContext(ioDispatcher) {
                try {
                    if (dao.insertIfNoneOpen(purchase.toEntity(), OPEN_STATUSES)) {
                        DataResult.Success(Unit)
                    } else {
                        DataResult.Failure(TicketingError.PurchaseAlreadyOpen)
                    }
                } catch (failure: SQLiteException) {
                    storageFailure("create_pending", failure)
                }
            }

        override suspend fun update(purchase: Purchase): DataResult<Unit> =
            withContext(ioDispatcher) {
                try {
                    if (dao.update(purchase.toEntity()) > 0) {
                        DataResult.Success(Unit)
                    } else {
                        DataResult.Failure(TicketingError.StorageFailure)
                    }
                } catch (failure: SQLiteException) {
                    storageFailure("update", failure)
                }
            }

        override suspend fun findOpen(): DataResult<Purchase?> =
            withContext(ioDispatcher) {
                try {
                    guarded("find_open") { dao.findOpen(OPEN_STATUSES)?.toDomain() }
                } catch (failure: SQLiteException) {
                    storageFailure("find_open", failure)
                }
            }

        override suspend fun findById(id: String): DataResult<Purchase?> =
            withContext(ioDispatcher) {
                try {
                    guarded("find_by_id") { dao.findById(id)?.toDomain() }
                } catch (failure: SQLiteException) {
                    storageFailure("find_by_id", failure)
                }
            }

        private inline fun <T> guarded(
            operation: String,
            block: () -> T,
        ): DataResult<T> =
            try {
                DataResult.Success(block())
            } catch (failure: IllegalArgumentException) {
                storageFailure(operation, failure)
            }

        private fun <T> Flow<DataResult<T>>.recoverStorage(operation: String): Flow<DataResult<T>> =
            catch { failure ->
                if (failure is SQLiteException) {
                    emit(storageFailure(operation, failure))
                } else {
                    throw failure
                }
            }.flowOn(ioDispatcher)

        private fun storageFailure(
            operation: String,
            cause: Throwable,
        ): DataResult.Failure {
            logger.error("purchase_storage_failure(operation=$operation)", cause)
            return DataResult.Failure(TicketingError.StorageFailure)
        }

        private companion object {
            val OPEN_STATUSES: List<String> = PurchaseStatus.entries.filter { it.isOpen }.map { it.name }
            val SALE_STATUSES: List<String> = PurchaseStatus.entries.filter { it.isSale }.map { it.name }
        }
    }
