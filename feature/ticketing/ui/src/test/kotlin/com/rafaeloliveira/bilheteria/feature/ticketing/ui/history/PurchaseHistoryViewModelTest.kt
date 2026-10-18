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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.history

import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseHistoryUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Duration

class PurchaseHistoryViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(repository: FakePurchaseRepository) =
        PurchaseHistoryViewModel(ObservePurchaseHistoryUseCase(repository))

    @Test
    fun `AC-18 - dado compras registradas quando abrir historico entao ordem decrescente sem falhas`() =
        runTest {
            val oldest = purchase(REF_1, PurchaseStatus.Approved, minutesAgo = 90)
            val newest = purchase(REF_2, PurchaseStatus.Declined, minutesAgo = 5)
            val middle = purchase(REF_3, PurchaseStatus.Cancelled, minutesAgo = 30)
            val failed = purchase(REF_4, PurchaseStatus.Failed, minutesAgo = 1)
            val open = purchase(REF_5, PurchaseStatus.Pending, minutesAgo = 0)
            val repository = FakePurchaseRepository(listOf(oldest, failed, middle, open, newest))

            viewModel(repository).uiState.test {
                assertEquals(PurchaseHistoryUiState.Loading, awaitItem())
                val content = awaitItem() as PurchaseHistoryUiState.Content
                assertEquals(listOf(newest.id, middle.id, oldest.id), content.items.map { it.id })
                assertEquals(
                    listOf(PurchaseStatus.Declined, PurchaseStatus.Cancelled, PurchaseStatus.Approved),
                    content.items.map { it.status },
                )
            }
        }

    @Test
    fun `AC-18 - dado compra aprovada quando abrir historico entao item traz evento quantidade total e data`() =
        runTest {
            val approved = purchase(REF_1, PurchaseStatus.Approved, minutesAgo = 10)
            val repository = FakePurchaseRepository(listOf(approved))

            viewModel(repository).uiState.test {
                skipItems(1) // Loading
                val content = awaitItem() as PurchaseHistoryUiState.Content
                assertEquals(
                    HistoryItem(
                        id = approved.id,
                        eventName = approved.eventName,
                        createdAt = TEST_NOW.minus(Duration.ofMinutes(10)),
                        quantity = 2,
                        total = Money(10_000),
                        status = PurchaseStatus.Approved,
                    ),
                    content.items.single(),
                )
            }
        }

    @Test
    fun `AC-20 - dado nenhuma compra quando abrir historico entao exibe vazio`() =
        runTest {
            viewModel(FakePurchaseRepository()).uiState.test {
                assertEquals(PurchaseHistoryUiState.Loading, awaitItem())
                assertEquals(PurchaseHistoryUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `AC-20 - dado apenas tentativas que nao sao vendas quando abrir historico entao exibe vazio`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(purchase(REF_1, PurchaseStatus.Failed, minutesAgo = 3)))

            viewModel(repository).uiState.test {
                skipItems(1) // Loading
                assertEquals(PurchaseHistoryUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `AC-21 - dado falha ao ler historico quando abrir entao exibe erro`() =
        runTest {
            val repository = FakePurchaseRepository().apply { failStorage = true }

            viewModel(repository).uiState.test {
                assertEquals(PurchaseHistoryUiState.Loading, awaitItem())
                assertEquals(PurchaseHistoryUiState.Error, awaitItem())
            }
        }

    @Test
    fun `AC-21 - dado erro de leitura quando tentar de novo entao volta a carregar e exibe o historico`() =
        runTest {
            val approved = purchase(REF_1, PurchaseStatus.Approved, minutesAgo = 10)
            val repository = FakePurchaseRepository(listOf(approved)).apply { failStorage = true }
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                skipItems(1) // Loading
                assertEquals(PurchaseHistoryUiState.Error, awaitItem())

                repository.failStorage = false
                viewModel.onEvent(PurchaseHistoryUiEvent.OnRetry)

                assertEquals(PurchaseHistoryUiState.Loading, awaitItem())
                val content = awaitItem() as PurchaseHistoryUiState.Content
                assertEquals(listOf(approved.id), content.items.map { it.id })
            }
        }

    @Test
    fun `AC-21 - dado erro de leitura persistente quando tentar de novo entao volta a carregar e exibe erro`() =
        runTest {
            val repository = FakePurchaseRepository().apply { failStorage = true }
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                skipItems(1) // Loading
                assertEquals(PurchaseHistoryUiState.Error, awaitItem())

                viewModel.onEvent(PurchaseHistoryUiEvent.OnRetry)

                assertEquals(PurchaseHistoryUiState.Loading, awaitItem())
                assertEquals(PurchaseHistoryUiState.Error, awaitItem())
            }
        }

    @Test
    fun `dado historico exibido quando tentar de novo entao nao recarrega`() =
        runTest {
            val approved = purchase(REF_1, PurchaseStatus.Approved, minutesAgo = 10)
            val viewModel = viewModel(FakePurchaseRepository(listOf(approved)))

            viewModel.uiState.test {
                skipItems(1) // Loading
                val content = awaitItem()

                viewModel.onEvent(PurchaseHistoryUiEvent.OnRetry)

                expectNoEvents()
                assertEquals(content, viewModel.uiState.value)
            }
        }

    @Test
    fun `dado historico com compras quando selecionar uma compra entao o estado nao muda`() =
        runTest {
            val approved = purchase(REF_1, PurchaseStatus.Approved, minutesAgo = 10)
            val viewModel = viewModel(FakePurchaseRepository(listOf(approved)))

            viewModel.uiState.test {
                skipItems(1) // Loading
                val content = awaitItem()
                assertTrue(content is PurchaseHistoryUiState.Content)

                viewModel.onEvent(PurchaseHistoryUiEvent.OnPurchaseSelected(approved.id))

                expectNoEvents()
                assertEquals(content, viewModel.uiState.value)
            }
        }

    private fun purchase(
        reference: String,
        status: PurchaseStatus,
        minutesAgo: Long,
    ) = aPurchase(
        reference = reference,
        event = anEvent(),
        quantity = 2,
        status = status,
        transaction = if (status == PurchaseStatus.Approved) aTransaction() else null,
        createdAt = TEST_NOW.minus(Duration.ofMinutes(minutesAgo)),
    )

    private companion object {
        const val REF_1 = "history-purchase-1"
        const val REF_2 = "history-purchase-2"
        const val REF_3 = "history-purchase-3"
        const val REF_4 = "history-purchase-4"
        const val REF_5 = "history-purchase-5"
    }
}
