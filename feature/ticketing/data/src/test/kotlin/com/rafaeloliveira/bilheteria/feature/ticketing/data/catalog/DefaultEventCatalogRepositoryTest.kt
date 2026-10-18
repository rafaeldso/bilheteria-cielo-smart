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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEventCatalogRepositoryTest {
    private val dispatcher = StandardTestDispatcher()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/catalog/$name")) { "Missing fixture $name" }
            .use { it.readBytes().decodeToString() }

    private fun repository(source: CatalogTextSource) = DefaultEventCatalogRepository(source, dispatcher)

    private fun textOf(text: String) = CatalogTextSource { text }

    private fun failingWith(error: Exception) = CatalogTextSource { throw error }

    @Test
    fun `AC-01 - dado catalogo valido quando observar eventos entao emite os eventos mapeados`() =
        runTest(dispatcher) {
            val result = repository(textOf(fixture("valid.json"))).observeEvents().first()

            val expected =
                listOf(
                    Event(
                        id = "t-1",
                        name = "Show de Teste",
                        startsAt = Instant.parse("2026-11-14T23:00:00Z"),
                        venue = "Teatro de Teste",
                        unitPrice = Money(8_000),
                        totalTickets = 10,
                    ),
                    Event(
                        id = "t-2",
                        name = "Feira de Teste",
                        startsAt = Instant.parse("2027-01-10T09:30:00Z"),
                        venue = "Praca de Teste",
                        unitPrice = Money(0),
                        totalTickets = 0,
                    ),
                )
            assertEquals(DataResult.Success(expected), result)
        }

    @Test
    fun `AC-01 - dado catalogo vazio quando observar eventos entao emite lista vazia sem erro`() =
        runTest(dispatcher) {
            val result = repository(textOf("[]")).observeEvents().first()

            assertEquals(DataResult.Success(emptyList<Event>()), result)
        }

    @Test
    fun `AC-03 - dado json malformado quando observar eventos entao falha com catalogo indisponivel`() =
        runTest(dispatcher) {
            val result = repository(textOf(fixture("malformed.json"))).observeEvents().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), result)
        }

    @Test
    fun `AC-03 - dado campo obrigatorio ausente quando observar eventos entao falha com catalogo indisponivel`() =
        runTest(dispatcher) {
            val result = repository(textOf(fixture("missing-field.json"))).observeEvents().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), result)
        }

    @Test
    fun `AC-03 - dado data invalida no json quando observar eventos entao falha com catalogo indisponivel`() =
        runTest(dispatcher) {
            val result = repository(textOf(fixture("invalid-date.json"))).observeEvents().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), result)
        }

    @Test
    fun `AC-03 - dado asset ausente quando observar eventos entao falha com catalogo indisponivel`() =
        runTest(dispatcher) {
            val source = failingWith(FileNotFoundException("events.json"))

            val result = repository(source).observeEvents().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), result)
        }

    @Test
    fun `AC-03 - dado raiz do json que nao e lista quando observar eventos entao falha com catalogo indisponivel`() =
        runTest(dispatcher) {
            val result = repository(textOf("""{"id":"t-1"}""")).observeEvents().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), result)
        }

    @Test
    fun `AC-03 - dado leitura que falhou quando observar de novo e a leitura funcionar entao emite a lista`() =
        runTest(dispatcher) {
            val texts = ArrayDeque(listOf<String?>(null, fixture("valid.json")))
            val source =
                CatalogTextSource { texts.removeFirst() ?: throw FileNotFoundException("events.json") }
            val repository = repository(source)

            val first = repository.observeEvents().first()
            val retry = repository.observeEvents().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), first)
            assertTrue(retry is DataResult.Success && retry.value.size == 2)
        }
}
