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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.common.IoDispatcher
import com.rafaeloliveira.bilheteria.core.common.suspendRunCatching
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.EventCatalogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Catalog read from the local JSON (BR-01).
 *
 * The flow is cold: every collection reads the source again, so "Tentar novamente" (AC-03) is a new
 * collection. Any failure (missing asset, malformed JSON, invalid date or value) becomes
 * `TicketingError.CatalogUnavailable` (BR-04, plan §5); the cause is not exposed.
 */
internal class DefaultEventCatalogRepository
    @Inject
    constructor(
        private val source: CatalogTextSource,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : EventCatalogRepository {
        override fun observeEvents(): Flow<DataResult<List<Event>>> = flow { emit(load()) }.flowOn(ioDispatcher)

        private suspend fun load(): DataResult<List<Event>> =
            suspendRunCatching {
                json.decodeFromString(ListSerializer(EventDto.serializer()), source.readText()).toDomain()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(TicketingError.CatalogUnavailable) },
            )

        private companion object {
            val json = Json
        }
    }
