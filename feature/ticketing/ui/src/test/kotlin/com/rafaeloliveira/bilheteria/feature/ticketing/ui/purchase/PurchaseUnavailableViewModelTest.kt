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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PurchaseUnavailableViewModelTest : PurchaseViewModelTestBase() {
    @Test
    fun `AC-26 - dado catalogo ilegivel e sem compra aberta quando abrir a compra entao exibe indisponivel`() =
        runTest {
            catalog.failure()
            val viewModel = createViewModel(repositoryWith())
            observing(viewModel) {
                assertEquals(PurchaseUiState.Unavailable, latest(viewModel))

                viewModel.onEvent(PurchaseUiEvent.OnPay)
                latest(viewModel)

                assertTrue(gateway.paymentRequests.isEmpty())
            }
        }

    @Test
    fun `AC-26 - dado evento inexistente e sem compra aberta quando abrir a compra entao exibe indisponivel`() =
        runTest {
            val viewModel = createViewModel(repositoryWith(), SavedStateHandle(mapOf("eventId" to "inexistente")))
            observing(viewModel) {
                assertEquals(PurchaseUiState.Unavailable, latest(viewModel))
            }
        }

    @Test
    fun `AC-26 - dado evento esgotado e sem compra aberta quando abrir a compra entao exibe indisponivel`() =
        runTest {
            val viewModel = createViewModel(repositoryWith(soldOut(remaining = 0)))
            observing(viewModel) {
                assertEquals(PurchaseUiState.Unavailable, latest(viewModel))
            }
        }

    @Test
    fun `AC-26 - dado evento ausente na rota e sem compra aberta quando abrir a compra entao exibe indisponivel`() =
        runTest {
            val viewModel = createViewModel(repositoryWith(), SavedStateHandle())
            observing(viewModel) {
                assertEquals(PurchaseUiState.Unavailable, latest(viewModel))
            }
        }

    @Test
    fun `AC-26 - dado catalogo ilegivel e compra em andamento quando abrir a compra entao prevalece a compra`() =
        runTest {
            catalog.failure()
            val pending = aPurchase(reference = FIRST_REFERENCE, event = event, quantity = 2)
            val viewModel = createViewModel(repositoryWith(pending))
            observing(viewModel) {
                val state = latest(viewModel)

                assertTrue(state is PurchaseUiState.Recovering)
                assertTrue(gateway.paymentRequests.isEmpty())
            }
        }
}
