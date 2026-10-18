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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.time.OffsetDateTime

/**
 * Maps a catalog entry to the domain model.
 *
 * Throws [java.time.format.DateTimeParseException] for an invalid `startsAt` and
 * [IllegalArgumentException] for blank text, negative price or negative stock. The repository turns
 * any of them into `TicketingError.CatalogUnavailable` (plan §5).
 */
internal fun EventDto.toDomain(): Event {
    require(id.isNotBlank()) { "Event id is blank" }
    require(name.isNotBlank()) { "Event name is blank (id=$id)" }
    require(venue.isNotBlank()) { "Event venue is blank (id=$id)" }
    require(totalTickets >= 0) { "Event stock is negative (id=$id)" }
    return Event(
        id = id,
        name = name,
        startsAt = OffsetDateTime.parse(startsAt).toInstant(),
        venue = venue,
        unitPrice = Money(priceCents),
        totalTickets = totalTickets,
    )
}

/** Maps the whole catalog; duplicate ids make the catalog invalid because the id is the purchase key. */
internal fun List<EventDto>.toDomain(): List<Event> {
    val events = map(EventDto::toDomain)
    require(events.map(Event::id).toSet().size == events.size) { "Duplicate event ids in catalog" }
    return events
}
