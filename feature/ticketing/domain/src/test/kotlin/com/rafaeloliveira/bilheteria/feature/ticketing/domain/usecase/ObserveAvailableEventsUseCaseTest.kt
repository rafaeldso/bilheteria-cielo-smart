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

import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeEventCatalogRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ObserveAvailableEventsUseCaseTest {
    private val clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)

    private val past = anEvent(id = "past", startsAt = Instant.parse("2026-09-01T20:00:00Z"))
    private val november = anEvent(id = "nov", startsAt = Instant.parse("2026-11-15T23:00:00Z"))
    private val december = anEvent(id = "dec", startsAt = Instant.parse("2026-12-10T23:00:00Z"))

    private fun useCase(
        catalog: FakeEventCatalogRepository,
        purchases: PurchaseRepository = FakePurchaseRepository(),
    ) = ObserveAvailableEventsUseCase(catalog, purchases, clock)

    private suspend fun ObserveAvailableEventsUseCase.events(): List<AvailableEvent> =
        (invoke().first() as DataResult.Success).value

    @Test
    fun `AC-01 - dado eventos futuros e passados quando abrir entao lista so os disponiveis`() =
        runTest {
            val result = useCase(FakeEventCatalogRepository(listOf(past, november, december))).events()

            assertEquals(listOf("nov", "dec"), result.map { it.event.id })
        }

    @Test
    fun `AC-01 - dado evento que comeca exatamente agora quando abrir entao nao aparece`() =
        runTest {
            val startingNow = anEvent(id = "now", startsAt = TEST_NOW)
            val oneSecondAhead = anEvent(id = "soon", startsAt = TEST_NOW.plusSeconds(1))

            val result = useCase(FakeEventCatalogRepository(listOf(startingNow, oneSecondAhead))).events()

            assertEquals(listOf("soon"), result.map { it.event.id })
        }

    @Test
    fun `AC-02 - dado catalogo sem eventos disponiveis quando abrir entao exibe vazio`() =
        runTest {
            assertEquals(emptyList<AvailableEvent>(), useCase(FakeEventCatalogRepository(emptyList())).events())
            assertEquals(emptyList<AvailableEvent>(), useCase(FakeEventCatalogRepository(listOf(past))).events())
        }

    @Test
    fun `AC-02 - dado unico evento futuro esgotado quando abrir entao exibe vazio`() =
        runTest {
            val soldOut = aPurchase(event = november, quantity = 50, status = PurchaseStatus.Approved)

            val result = useCase(FakeEventCatalogRepository(listOf(november)), FakePurchaseRepository(listOf(soldOut)))

            assertEquals(emptyList<AvailableEvent>(), result.events())
        }

    @Test
    fun `AC-17 - dado 50 ingressos quando aprovar 1 entao mostra 49`() =
        runTest {
            val approved = aPurchase(event = november, quantity = 1, status = PurchaseStatus.Approved)

            val result = useCase(FakeEventCatalogRepository(listOf(november)), FakePurchaseRepository(listOf(approved)))

            assertEquals(listOf(AvailableEvent(november, available = 49)), result.events())
        }

    @Test
    fun `AC-17 - dado compra pendente quando aprovar entao a lista reage e mostra 48`() =
        runTest {
            val pending = aPurchase(event = november, quantity = 2, status = PurchaseStatus.Pending)
            val purchases = FakePurchaseRepository(listOf(pending))

            useCase(FakeEventCatalogRepository(listOf(november)), purchases).invoke().test {
                assertEquals(50, ((awaitItem() as DataResult.Success).value.single()).available)
                purchases.update(pending.copy(status = PurchaseStatus.Approved))
                assertEquals(48, ((awaitItem() as DataResult.Success).value.single()).available)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @EnumSource(PurchaseStatus::class, names = ["Approved"], mode = EnumSource.Mode.EXCLUDE)
    fun `AC-17 - dado compra que nao foi aprovada quando listar entao nao reduz disponiveis`(status: PurchaseStatus) =
        runTest {
            val purchase = aPurchase(event = november, quantity = 5, status = status)

            val result = useCase(FakeEventCatalogRepository(listOf(november)), FakePurchaseRepository(listOf(purchase)))

            assertEquals(50, result.events().single().available)
        }

    @Test
    fun `AC-17 - dado varias aprovadas do mesmo evento quando listar entao soma so as do evento`() =
        runTest {
            val purchases =
                FakePurchaseRepository(
                    listOf(
                        aPurchase(reference = "r1", event = november, quantity = 3, status = PurchaseStatus.Approved),
                        aPurchase(reference = "r2", event = november, quantity = 4, status = PurchaseStatus.Approved),
                        aPurchase(reference = "r3", event = december, quantity = 9, status = PurchaseStatus.Approved),
                    ),
                )

            val result = useCase(FakeEventCatalogRepository(listOf(november, december)), purchases).events()

            assertEquals(mapOf("nov" to 43, "dec" to 41), result.associate { it.event.id to it.available })
        }

    @Test
    fun `AC-17 - dado aprovadas alem do total quando listar entao disponiveis nunca fica negativo`() =
        runTest {
            val oversold = aPurchase(event = november, quantity = 51, status = PurchaseStatus.Approved)

            val result = useCase(FakeEventCatalogRepository(listOf(november)), FakePurchaseRepository(listOf(oversold)))

            assertEquals(emptyList<AvailableEvent>(), result.events())
        }

    @Test
    fun `AC-17 - dado ultimo ingresso quando aprovar entao mostra 1 e o evento segue na lista`() =
        runTest {
            val approved = aPurchase(event = november, quantity = 49, status = PurchaseStatus.Approved)

            val result = useCase(FakeEventCatalogRepository(listOf(november)), FakePurchaseRepository(listOf(approved)))

            assertEquals(1, result.events().single().available)
        }

    @Test
    fun `AC-22 - dado catalogo fora de ordem quando abrir entao ordena por data do evento`() =
        runTest {
            val result = useCase(FakeEventCatalogRepository(listOf(december, november))).events()

            assertEquals(listOf("nov", "dec"), result.map { it.event.id })
        }

    @Test
    fun `AC-01 - dado falha no catalogo quando abrir entao emite a falha`() =
        runTest {
            val catalog = FakeEventCatalogRepository(listOf(november)).also { it.failure() }

            val result = useCase(catalog).invoke().first()

            assertEquals(DataResult.Failure(TicketingError.CatalogUnavailable), result)
        }

    @Test
    fun `AC-17 - dado falha ao ler compras aprovadas quando abrir entao emite a falha`() =
        runTest {
            val purchases = mockk<PurchaseRepository>()
            every { purchases.observeApprovedQuantities() } returns
                flowOf(DataResult.Failure(TicketingError.StorageFailure))

            val result = useCase(FakeEventCatalogRepository(listOf(november)), purchases).invoke().first()

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
        }

    @Test
    fun `AC-02 - dado catalogo recuperado depois da falha quando emitir eventos entao volta a listar`() =
        runTest {
            val catalog = FakeEventCatalogRepository(emptyList())

            useCase(catalog).invoke().test {
                assertEquals(emptyList<Event>(), (awaitItem() as DataResult.Success).value)
                catalog.failure()
                assertInstanceOf(DataResult.Failure::class.java, awaitItem())
                catalog.emit(listOf(november))
                assertEquals(1, (awaitItem() as DataResult.Success).value.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
