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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.catalog

import kotlinx.serialization.Serializable

/** Event as written in `assets/events.json`. Never leaves the data layer (AP-06). */
@Serializable
internal data class EventDto(
    val id: String,
    val name: String,
    /** ISO-8601 with offset, e.g. `2026-11-14T20:00:00-03:00`. */
    val startsAt: String,
    val venue: String,
    val priceCents: Long,
    val totalTickets: Int,
)
