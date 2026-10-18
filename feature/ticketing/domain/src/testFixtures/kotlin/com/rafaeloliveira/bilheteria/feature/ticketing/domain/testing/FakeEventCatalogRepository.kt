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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.EventCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory catalog. Set [failure] to simulate an unreadable catalog (BR-04). */
class FakeEventCatalogRepository(
    events: List<Event> = listOf(anEvent()),
) : EventCatalogRepository {
    private val state = MutableStateFlow<DataResult<List<Event>>>(DataResult.Success(events))

    fun emit(events: List<Event>) {
        state.value = DataResult.Success(events)
    }

    fun failure() {
        state.value = DataResult.Failure(TicketingError.CatalogUnavailable)
    }

    override fun observeEvents(): Flow<DataResult<List<Event>>> = state
}
