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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.eventlist

import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeEventCatalogRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.GroupEventsByMonthUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveAvailableEventsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class EventListViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)
    private val catalog = FakeEventCatalogRepository(emptyList())
    private val purchases = FakePurchaseRepository()

    private fun createViewModel() =
        EventListViewModel(
            observeAvailableEvents = ObserveAvailableEventsUseCase(catalog, purchases, clock),
            groupEventsByMonth = GroupEventsByMonthUseCase(clock),
        )

    @Test
    fun `AC-01 - dado eventos futuros e passados quando abrir entao lista so os disponiveis`() =
        runTest {
            val upcoming = anEvent(id = "futuro", name = "Noite de Jazz no Parque", totalTickets = 50)
            val past = anEvent(id = "passado", startsAt = Instant.parse("2026-10-01T20:00:00Z"))
            val soldOut = anEvent(id = "esgotado", startsAt = Instant.parse("2026-11-20T20:00:00Z"), totalTickets = 1)
            catalog.emit(listOf(upcoming, past, soldOut))
            purchases.approve(soldOut, quantity = 1)

            createViewModel().uiState.test {
                assertEquals(EventListUiState.Loading, awaitItem())
                val content = awaitItem() as EventListUiState.Content
                assertEquals(listOf(AvailableEvent(upcoming, available = 50)), content.groups.single().events)
            }
        }

    @Test
    fun `AC-02 - dado catalogo sem eventos disponiveis quando abrir entao exibe vazio`() =
        runTest {
            catalog.emit(listOf(anEvent(startsAt = Instant.parse("2026-10-01T20:00:00Z"))))

            createViewModel().uiState.test {
                skipItems(1) // Loading
                assertEquals(EventListUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `AC-03 - dado falha ao ler catalogo quando tentar de novo entao exibe lista`() =
        runTest {
            val event = anEvent()
            catalog.failure()
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(1) // Loading
                assertEquals(EventListUiState.Error, awaitItem())

                catalog.emit(listOf(event))
                viewModel.onEvent(EventListUiEvent.OnRetry)

                assertEquals(EventListUiState.Loading, awaitItem())
                val content = awaitItem() as EventListUiState.Content
                assertEquals(listOf(AvailableEvent(event, available = 50)), content.groups.single().events)
            }
        }

    @Test
    fun `AC-03 - dado falha persistente quando tentar de novo entao volta ao erro`() =
        runTest {
            catalog.failure()
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(2) // Loading, Error
                viewModel.onEvent(EventListUiEvent.OnRetry)

                assertEquals(EventListUiState.Loading, awaitItem())
                assertEquals(EventListUiState.Error, awaitItem())
            }
        }

    @Test
    fun `AC-17 - dado 50 ingressos quando aprovar 1 entao mostra 49`() =
        runTest {
            val event = anEvent(totalTickets = 50)
            catalog.emit(listOf(event))

            createViewModel().uiState.test {
                skipItems(1) // Loading
                assertEquals(50, availableOf(awaitItem()))

                purchases.approve(event, quantity = 1)

                assertEquals(49, availableOf(awaitItem()))
            }
        }

    @Test
    fun `AC-17 - dado 49 disponiveis quando compra e negada ou cancelada ou falha entao continua em 49`() =
        runTest {
            val event = anEvent(totalTickets = 50)
            catalog.emit(listOf(event))
            purchases.approve(event, quantity = 1)

            createViewModel().uiState.test {
                skipItems(1) // Loading
                assertEquals(49, availableOf(awaitItem()))

                purchases.settle(event, quantity = 2, status = PurchaseStatus.Declined, reference = "ref-declined")
                purchases.settle(event, quantity = 2, status = PurchaseStatus.Cancelled, reference = "ref-cancelled")
                purchases.settle(event, quantity = 2, status = PurchaseStatus.Failed, reference = "ref-failed")
                advanceUntilIdle()

                expectNoEvents()
            }
        }

    @Test
    fun `AC-22 - dado eventos em dois meses quando abrir entao agrupa por mes`() =
        runTest {
            val decLate = anEvent(id = "dez-tarde", startsAt = Instant.parse("2026-12-20T22:00:00Z"))
            val novLate = anEvent(id = "nov-tarde", startsAt = Instant.parse("2026-11-25T22:00:00Z"))
            val decEarly = anEvent(id = "dez-cedo", startsAt = Instant.parse("2026-12-05T18:00:00Z"))
            val novEarly = anEvent(id = "nov-cedo", startsAt = Instant.parse("2026-11-05T18:00:00Z"))
            catalog.emit(listOf(decLate, novLate, decEarly, novEarly))

            createViewModel().uiState.test {
                skipItems(1) // Loading
                val content = awaitItem() as EventListUiState.Content

                assertEquals(listOf(NOVEMBER, DECEMBER), content.groups.map { it.month })
                assertEquals(listOf("nov-cedo", "nov-tarde"), content.groups[0].events.map { it.event.id })
                assertEquals(listOf("dez-cedo", "dez-tarde"), content.groups[1].events.map { it.event.id })
            }
        }

    @Test
    fun `AC-23 - dado eventos em novembro e dezembro quando abrir entao filtro mostra todos e so os dois meses`() =
        runTest {
            catalog.emit(listOf(novemberEvent, decemberEvent))

            createViewModel().uiState.test {
                skipItems(1) // Loading
                val content = awaitItem() as EventListUiState.Content

                assertEquals(null, content.selectedMonth)
                assertEquals(listOf(MonthOption(NOVEMBER), MonthOption(DECEMBER)), content.months)
            }
        }

    @Test
    fun `AC-23 - dado filtro de mes quando selecionar dezembro entao so eventos de dezembro`() =
        runTest {
            catalog.emit(listOf(novemberEvent, decemberEvent))
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(2) // Loading, Content (Todos)

                viewModel.onEvent(EventListUiEvent.OnMonthSelected(DECEMBER))
                val december = awaitItem() as EventListUiState.Content
                assertEquals(DECEMBER, december.selectedMonth)
                assertEquals(listOf(DECEMBER), december.groups.map { it.month })
                assertEquals(
                    listOf(decemberEvent.id),
                    december.groups
                        .single()
                        .events
                        .map { it.event.id },
                )

                viewModel.onEvent(EventListUiEvent.OnMonthSelected(null))
                val all = awaitItem() as EventListUiState.Content
                assertEquals(null, all.selectedMonth)
                assertEquals(listOf(NOVEMBER, DECEMBER), all.groups.map { it.month })
                assertEquals(listOf(MonthOption(NOVEMBER), MonthOption(DECEMBER)), all.months)
            }
        }

    @Test
    fun `AC-24 - dado mes esgotado quando voltar a lista entao filtro volta para todos`() =
        runTest {
            val lastTicket =
                anEvent(id = "dez-ultimo", startsAt = Instant.parse("2026-12-10T20:00:00Z"), totalTickets = 1)
            catalog.emit(listOf(novemberEvent, lastTicket))
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(2) // Loading, Content (Todos)
                viewModel.onEvent(EventListUiEvent.OnMonthSelected(DECEMBER))
                assertEquals(DECEMBER, (awaitItem() as EventListUiState.Content).selectedMonth)

                purchases.approve(lastTicket, quantity = 1)

                val content = awaitItem() as EventListUiState.Content
                assertEquals(null, content.selectedMonth)
                assertEquals(listOf(MonthOption(NOVEMBER)), content.months)
                assertEquals(listOf(NOVEMBER), content.groups.map { it.month })
            }
        }

    @Test
    fun `AC-24 - dado filtro reiniciado em todos quando o mes volta a ter evento entao permanece em todos`() =
        runTest {
            val lastTicket =
                anEvent(id = "dez-ultimo", startsAt = Instant.parse("2026-12-10T20:00:00Z"), totalTickets = 1)
            catalog.emit(listOf(novemberEvent, lastTicket))
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(2) // Loading, Content (Todos)
                viewModel.onEvent(EventListUiEvent.OnMonthSelected(DECEMBER))
                skipItems(1) // Content (Dezembro)
                purchases.approve(lastTicket, quantity = 1)
                skipItems(1) // Content (Todos, sem dezembro)

                catalog.emit(listOf(novemberEvent, lastTicket, decemberEvent))

                val content = awaitItem() as EventListUiState.Content
                assertEquals(null, content.selectedMonth)
                assertEquals(listOf(NOVEMBER, DECEMBER), content.groups.map { it.month })
            }
        }

    @Test
    fun `dado mes sem evento quando selecionar entao permanece em todos`() =
        runTest {
            catalog.emit(listOf(novemberEvent))
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(2) // Loading, Content (Todos)

                viewModel.onEvent(EventListUiEvent.OnMonthSelected(DECEMBER))
                advanceUntilIdle()

                expectNoEvents()
            }
        }

    @Test
    fun `dado lista exibida quando selecionar evento ou abrir historico entao estado nao muda`() =
        runTest {
            catalog.emit(listOf(novemberEvent))
            val viewModel = createViewModel()

            viewModel.uiState.test {
                skipItems(2) // Loading, Content

                viewModel.onEvent(EventListUiEvent.OnEventSelected(novemberEvent.id))
                viewModel.onEvent(EventListUiEvent.OnOpenHistory)
                advanceUntilIdle()

                expectNoEvents()
            }
        }

    private fun availableOf(state: EventListUiState): Int =
        (state as EventListUiState.Content)
            .groups
            .single()
            .events
            .single()
            .available

    /** Records a purchase of [event] that ends in [status], going through the repository like the real flow (BR-10). */
    private suspend fun FakePurchaseRepository.settle(
        event: Event,
        quantity: Int,
        status: PurchaseStatus,
        reference: String,
    ) {
        val pending =
            aPurchase(reference = reference, event = event, quantity = quantity, status = PurchaseStatus.Pending)
        createPending(pending)
        update(pending.copy(status = status))
    }

    private suspend fun FakePurchaseRepository.approve(
        event: Event,
        quantity: Int,
    ) = settle(event, quantity, PurchaseStatus.Approved, reference = "ref-approved-${event.id}")

    private companion object {
        val NOVEMBER = EventMonth(2026, 11)
        val DECEMBER = EventMonth(2026, 12)
        val novemberEvent = anEvent(id = "nov", startsAt = Instant.parse("2026-11-15T23:00:00Z"))
        val decemberEvent = anEvent(id = "dez", startsAt = Instant.parse("2026-12-10T20:00:00Z"))
    }
}
