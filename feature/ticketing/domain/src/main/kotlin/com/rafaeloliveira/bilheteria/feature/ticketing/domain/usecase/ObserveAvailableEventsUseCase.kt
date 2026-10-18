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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.EventCatalogRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import javax.inject.Inject

/**
 * Upcoming events that still have tickets, ordered by date (BR-01, BR-02, BR-17, BR-22).
 * A failure from either source is emitted as is; it is never swallowed.
 */
class ObserveAvailableEventsUseCase
    @Inject
    constructor(
        private val catalog: EventCatalogRepository,
        private val purchases: PurchaseRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<DataResult<List<AvailableEvent>>> =
            combine(catalog.observeEvents(), purchases.observeApprovedQuantities()) { events, approved ->
                when {
                    events is DataResult.Failure -> {
                        events
                    }

                    approved is DataResult.Failure -> {
                        approved
                    }

                    events is DataResult.Success && approved is DataResult.Success -> {
                        DataResult.Success(available(events.value, approved.value))
                    }

                    else -> {
                        error("Unreachable: DataResult has only Success and Failure")
                    }
                }
            }

        private fun available(
            events: List<Event>,
            approved: Map<String, Int>,
        ): List<AvailableEvent> {
            val now = clock.instant()
            return events
                .filter { it.startsAt.isAfter(now) }
                .map { AvailableEvent(it, (it.totalTickets - (approved[it.id] ?: 0)).coerceAtLeast(0)) }
                .filter { it.available > 0 }
                .sortedBy { it.event.startsAt }
        }
    }
