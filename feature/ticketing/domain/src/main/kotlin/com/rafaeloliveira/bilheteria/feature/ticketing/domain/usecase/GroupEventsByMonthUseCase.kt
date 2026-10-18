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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

data class MonthGroup(
    val month: EventMonth,
    val events: List<AvailableEvent>,
)

/**
 * @property months months that have at least one event, in order (BR-23).
 * @property selected the active month filter, or null for "Todos" (BR-24).
 * @property groups every month when [selected] is null, otherwise only the selected one.
 */
data class EventsByMonth(
    val months: List<EventMonth>,
    val selected: EventMonth?,
    val groups: List<MonthGroup>,
)

/** Groups events by month in the clock's zone (BR-22, BR-23) and resets a stale filter to "Todos" (BR-24). */
class GroupEventsByMonthUseCase
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        operator fun invoke(
            events: List<AvailableEvent>,
            selected: EventMonth?,
        ): EventsByMonth {
            val zone = clock.zone
            val all =
                events
                    .sortedBy { it.event.startsAt }
                    .groupBy { monthOf(it, zone) }
                    .map { (month, items) -> MonthGroup(month, items) }
                    .sortedBy { it.month }
            val months = all.map { it.month }
            val effective = selected?.takeIf { it in months }
            return EventsByMonth(
                months = months,
                selected = effective,
                groups = if (effective == null) all else all.filter { it.month == effective },
            )
        }

        private fun monthOf(
            event: AvailableEvent,
            zone: ZoneId,
        ): EventMonth {
            val date = event.event.startsAt.atZone(zone)
            return EventMonth(date.year, date.monthValue)
        }
    }
