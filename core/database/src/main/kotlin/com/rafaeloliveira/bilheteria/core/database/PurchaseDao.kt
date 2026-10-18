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
package com.rafaeloliveira.bilheteria.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Access to the `purchase` table. Status names come from the caller (the domain enum), so this module
 * does not duplicate the status vocabulary.
 */
@Dao
abstract class PurchaseDao {
    @Query("SELECT * FROM purchase WHERE id = :id")
    abstract fun observeById(id: String): Flow<PurchaseEntity?>

    /** Newest first (BR-18); `id` breaks ties so the order is stable. */
    @Query("SELECT * FROM purchase WHERE status IN (:statuses) ORDER BY created_at DESC, id DESC")
    abstract fun observeByStatuses(statuses: List<String>): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchase WHERE status IN (:openStatuses) ORDER BY created_at DESC LIMIT 1")
    abstract fun observeOpen(openStatuses: List<String>): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchase WHERE status IN (:openStatuses) ORDER BY created_at DESC LIMIT 1")
    abstract suspend fun findOpen(openStatuses: List<String>): PurchaseEntity?

    @Query(
        "SELECT event_id, SUM(quantity) AS total_quantity FROM purchase " +
            "WHERE status = :status GROUP BY event_id",
    )
    abstract fun observeQuantitiesByStatus(status: String): Flow<List<EventQuantity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insert(entity: PurchaseEntity)

    /** Returns the number of rows updated: 0 when the purchase does not exist. */
    @Update
    abstract suspend fun update(entity: PurchaseEntity): Int

    /**
     * Inserts [entity] unless a purchase with a status in [openStatuses] already exists (BR-10).
     * The check and the insert share one transaction, so two callers cannot both win.
     * Returns `false` when another purchase is open.
     */
    @Transaction
    open suspend fun insertIfNoneOpen(
        entity: PurchaseEntity,
        openStatuses: List<String>,
    ): Boolean {
        if (findOpen(openStatuses) != null) return false
        insert(entity)
        return true
    }
}
