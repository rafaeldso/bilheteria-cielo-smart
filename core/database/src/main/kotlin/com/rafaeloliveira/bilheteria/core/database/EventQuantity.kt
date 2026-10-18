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

/** Sum of tickets of one event over the purchases with a given status (projection of [PurchaseDao]). */
data class EventQuantity(
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "total_quantity") val quantity: Int,
)
