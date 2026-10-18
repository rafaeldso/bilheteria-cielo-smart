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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.model

import java.time.Instant

/** Catalog event (Q4). `totalTickets` is the stock before any sale. */
data class Event(
    val id: String,
    val name: String,
    val startsAt: Instant,
    val venue: String,
    val unitPrice: Money,
    val totalTickets: Int,
)

/** Event with the tickets still available: catalog stock minus approved purchases (BR-17). */
data class AvailableEvent(
    val event: Event,
    val available: Int,
)

/** Calendar month used to group and filter events (BR-22..BR-24). */
data class EventMonth(
    val year: Int,
    val month: Int,
) : Comparable<EventMonth> {
    override fun compareTo(other: EventMonth): Int = compareValuesBy(this, other, EventMonth::year, EventMonth::month)
}
