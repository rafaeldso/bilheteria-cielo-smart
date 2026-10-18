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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

/** Guards the real `assets/events.json` shipped in the APK (BR-22..BR-24 need 3+ months of events). */
@OptIn(ExperimentalCoroutinesApi::class)
class BundledCatalogAssetTest {
    private val dispatcher = StandardTestDispatcher()

    // Unit tests run with the module directory as working dir; the file is declared as a task input.
    private val assetFile = File("src/main/assets/events.json")

    private suspend fun bundledEvents(): List<Event> {
        val result =
            DefaultEventCatalogRepository(
                source = { assetFile.readText() },
                ioDispatcher = dispatcher,
            ).observeEvents().first()
        return (result as DataResult.Success).value
    }

    @Test
    fun `AC-01 - dado o asset real quando ler entao tem ao menos 7 eventos e ids unicos`() =
        runTest(dispatcher) {
            val events = bundledEvents()

            assertTrue(events.size >= MIN_EVENTS, "Expected at least $MIN_EVENTS events, got ${events.size}")
            assertEquals(events.size, events.map { it.id }.toSet().size)
        }

    @Test
    fun `AC-01 - dado o asset real quando ler entao todos os eventos ficam entre nov 2026 e mar 2027`() =
        runTest(dispatcher) {
            val events = bundledEvents()

            assertTrue(events.all { it.startsAt >= FIRST_DAY && it.startsAt < LAST_DAY_EXCLUSIVE })
        }

    @Test
    fun `AC-01 - dado o asset real quando ler entao cobre pelo menos 3 meses distintos`() =
        runTest(dispatcher) {
            val months =
                bundledEvents()
                    .map { it.startsAt.atOffset(ZoneOffset.UTC).let { date -> date.year to date.monthValue } }
                    .toSet()

            assertTrue(months.size >= MIN_MONTHS, "Expected at least $MIN_MONTHS months, got $months")
        }

    @Test
    fun `AC-01 - dado o asset real quando ler entao tem um evento com estoque pequeno para esgotar`() =
        runTest(dispatcher) {
            val events = bundledEvents()

            assertTrue(events.any { it.totalTickets in 1..SMALL_STOCK })
        }

    private companion object {
        const val MIN_EVENTS = 7
        const val MIN_MONTHS = 3
        const val SMALL_STOCK = 10
        val FIRST_DAY: Instant = Instant.parse("2026-11-01T00:00:00Z")
        val LAST_DAY_EXCLUSIVE: Instant = Instant.parse("2027-04-01T00:00:00Z")
    }
}
