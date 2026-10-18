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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase

import androidx.room.Room
import com.rafaeloliveira.bilheteria.core.database.BilheteriaDatabase
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.rules.ExternalResource
import org.robolectric.RuntimeEnvironment

/**
 * Builds in-memory Room databases whose queries run on [queryDispatcher] (the test dispatcher) instead of Room's
 * own threads, so a test never waits for a real thread and does not depend on timing. Every database built is
 * closed after the test, whether it passes or fails.
 */
class InMemoryDatabaseRule(
    private val queryDispatcher: CoroutineDispatcher,
) : ExternalResource() {
    private val databases = mutableListOf<BilheteriaDatabase>()

    fun newDatabase(): BilheteriaDatabase =
        Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BilheteriaDatabase::class.java)
            .setQueryCoroutineContext(queryDispatcher)
            .build()
            .also { databases += it }

    override fun after() {
        databases.forEach { it.close() }
        databases.clear()
    }
}
