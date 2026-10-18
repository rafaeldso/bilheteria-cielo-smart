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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Row of the `purchase` table (plan §4, Persistência). Enums are stored by name and instants as epoch
 * milliseconds; the data layer of the feature maps them. No card data exists here: Cielo never sends the
 * PAN to the app (BR-16), only the order, authorization code, NSU and brand.
 */
@Entity(tableName = "purchase")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "event_name") val eventName: String,
    @ColumnInfo(name = "event_starts_at") val eventStartsAt: Long,
    val venue: String,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long,
    val quantity: Int,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    val status: String,
    @ColumnInfo(name = "failure_reason") val failureReason: String?,
    @ColumnInfo(name = "cielo_order_id") val cieloOrderId: String?,
    @ColumnInfo(name = "auth_code") val authCode: String?,
    val nsu: String?,
    val brand: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
