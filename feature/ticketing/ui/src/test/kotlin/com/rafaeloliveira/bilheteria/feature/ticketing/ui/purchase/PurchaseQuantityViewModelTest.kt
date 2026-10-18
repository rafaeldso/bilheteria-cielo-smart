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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PurchaseQuantityViewModelTest : PurchaseViewModelTestBase() {
    @Test
    fun `dado ViewModel recem criado quando os dados chegarem entao passa de Loading para Editing`() =
        runTest {
            val viewModel = createViewModel(repositoryWith())
            viewModel.uiState.test {
                assertEquals(PurchaseUiState.Loading, awaitItem())
                advanceUntilIdle()
                assertTrue(awaitItem() is PurchaseUiState.Editing)
            }
        }

    @Test
    fun `AC-04 - dado evento de 50 reais quando selecionar entao quantidade 1 e total 50`() =
        runTest {
            val viewModel = createViewModel(repositoryWith())
            observing(viewModel) {
                val expected =
                    PurchaseUiState.Editing(
                        event = summary(),
                        quantity = 1,
                        canDecrement = false,
                        canIncrement = true,
                        limitHint = null,
                        total = Money(UNIT_PRICE_CENTS),
                    )
                assertEquals(expected, latest(viewModel))
            }
        }

    @Test
    fun `AC-05 - dado quantidade 1 quando aumentar para 3 entao total 150`() =
        runTest {
            val viewModel = createViewModel(repositoryWith())
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnIncrement)
                viewModel.onEvent(PurchaseUiEvent.OnIncrement)

                val state = latest(viewModel) as PurchaseUiState.Editing

                assertEquals(3, state.quantity)
                assertEquals(Money(THREE_TICKETS_CENTS), state.total)
                assertTrue(state.canDecrement)
            }
        }

    @Test
    fun `AC-05 - dado quantidade 3 salva quando recriar com o mesmo estado entao mantem 3 e total 150`() =
        runTest {
            val handle = SavedStateHandle(mapOf("eventId" to event.id))
            val first = createViewModel(repositoryWith(), handle)
            observing(first) {
                latest(first)
                first.onEvent(PurchaseUiEvent.OnIncrement)
                first.onEvent(PurchaseUiEvent.OnIncrement)
                latest(first)
            }

            val second = createViewModel(repositoryWith(), handle)
            observing(second) {
                val state = latest(second) as PurchaseUiState.Editing
                assertEquals(3, state.quantity)
                assertEquals(Money(THREE_TICKETS_CENTS), state.total)
            }
            assertEquals(3, handle.get<Int>("quantity"))
        }

    @Test
    fun `AC-06 - dado quantidade 1 quando tentar diminuir entao continua 1 e diminuir fica indisponivel`() =
        runTest {
            val viewModel = createViewModel(repositoryWith())
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnDecrement)

                val state = latest(viewModel) as PurchaseUiState.Editing

                assertEquals(1, state.quantity)
                assertEquals(false, state.canDecrement)
            }
        }

    @Test
    fun `AC-06 - dado quantidade 10 com 50 disponiveis quando aumentar entao continua 10 sem aumentar`() =
        runTest {
            val handle = SavedStateHandle(mapOf("eventId" to event.id, "quantity" to 10))
            val viewModel = createViewModel(repositoryWith(), handle)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnIncrement)

                val state = latest(viewModel) as PurchaseUiState.Editing

                assertEquals(10, state.quantity)
                assertEquals(false, state.canIncrement)
                assertEquals(LimitHint.MaxPerPurchase(10), state.limitHint)
            }
        }

    @Test
    fun `AC-06 - dado quantidade 3 com 3 disponiveis quando aumentar entao continua 3 sem aumentar`() =
        runTest {
            val sold = soldOut(remaining = 3)
            val viewModel = createViewModel(repositoryWith(sold))
            observing(viewModel) {
                latest(viewModel)
                repeat(4) { viewModel.onEvent(PurchaseUiEvent.OnIncrement) }

                val state = latest(viewModel) as PurchaseUiState.Editing

                assertEquals(3, state.quantity)
                assertEquals(false, state.canIncrement)
                assertEquals(LimitHint.OnlyAvailable(3), state.limitHint)
            }
        }

    @Test
    fun `AC-06 - dado quantidade salva 10 quando restam 3 ingressos entao oferece 3`() =
        runTest {
            val sold = soldOut(remaining = 3)
            val handle = SavedStateHandle(mapOf("eventId" to event.id, "quantity" to 10))
            val viewModel = createViewModel(repositoryWith(sold), handle)
            observing(viewModel) {
                val state = latest(viewModel) as PurchaseUiState.Editing

                assertEquals(3, state.quantity)
                assertEquals(Money(3 * UNIT_PRICE_CENTS), state.total)
            }
        }
}
