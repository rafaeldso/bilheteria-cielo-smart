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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReceiptViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakePurchaseRepository()

    private fun createViewModel(
        purchaseId: String?,
        source: PurchaseRepository = repository,
    ) = ReceiptViewModel(
        savedStateHandle = SavedStateHandle(purchaseId?.let { mapOf("purchaseId" to it) }.orEmpty()),
        observePurchase = ObservePurchaseUseCase(source),
    )

    @Test
    fun `AC-09 - dado compra em andamento quando a Cielo aprova entao exibe o comprovante aprovado`() =
        runTest {
            val pending = aPurchase(status = PurchaseStatus.Pending)
            repository.createPending(pending)

            createViewModel(pending.id).uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                repository.update(pending.copy(status = PurchaseStatus.Approved, transaction = aTransaction()))
                assertEquals(ReceiptUiState.Approved(expectedReceipt(pending, withTransaction = true)), awaitItem())
            }
        }

    @Test
    fun `AC-10 - dado compra em andamento quando o pagamento e cancelado no terminal entao exibe o resumo cancelado`() =
        runTest {
            val pending = aPurchase(status = PurchaseStatus.Pending)
            repository.createPending(pending)

            createViewModel(pending.id).uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                repository.update(pending.copy(status = PurchaseStatus.Cancelled))
                assertEquals(ReceiptUiState.Cancelled(expectedReceipt(pending, withTransaction = false)), awaitItem())
            }
        }

    @Test
    fun `AC-11 - dado compra em andamento quando a Cielo nega o pagamento entao exibe o resumo negado`() =
        runTest {
            val pending = aPurchase(status = PurchaseStatus.Pending)
            repository.createPending(pending)

            createViewModel(pending.id).uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                repository.update(pending.copy(status = PurchaseStatus.Declined))
                assertEquals(ReceiptUiState.Declined(expectedReceipt(pending, withTransaction = false)), awaitItem())
            }
        }

    @Test
    fun `AC-16 - dado compra aprovada quando exibir comprovante entao mostra os campos da compra e da transacao`() =
        runTest {
            val purchasedAt = Instant.parse("2026-10-17T14:32:00Z")
            val approved =
                aPurchase(
                    event = anEvent(name = "Stand-up Noite Livre", venue = "Teatro Central", unitPriceCents = 6_000),
                    quantity = 3,
                    status = PurchaseStatus.Approved,
                    transaction = aTransaction(),
                    createdAt = purchasedAt,
                )
            repository.createPending(approved)

            createViewModel(approved.id).uiState.test {
                skipItems(1) // Loading
                assertEquals(
                    ReceiptUiState.Approved(
                        ReceiptData(
                            purchaseNumber = approved.reference.value,
                            eventName = "Stand-up Noite Livre",
                            eventStartsAt = approved.eventStartsAt,
                            venue = "Teatro Central",
                            quantity = 3,
                            unitPrice = Money(6_000),
                            total = Money(18_000),
                            purchasedAt = purchasedAt,
                            transaction = ReceiptTransaction(brand = "Visa", authCode = "A1B2C3", nsu = "000123456"),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `AC-16 - dado compra negada com dados de transacao quando exibir resumo entao omite dados da transacao`() =
        runTest {
            val declined = aPurchase(status = PurchaseStatus.Declined, transaction = aTransaction())
            repository.createPending(declined)

            createViewModel(declined.id).uiState.test {
                skipItems(1) // Loading
                assertEquals(ReceiptUiState.Declined(expectedReceipt(declined, withTransaction = false)), awaitItem())
            }
        }

    @Test
    fun `AC-16 - dado compra cancelada com dados de transacao quando exibir resumo entao omite dados da transacao`() =
        runTest {
            val cancelled = aPurchase(status = PurchaseStatus.Cancelled, transaction = aTransaction())
            repository.createPending(cancelled)

            createViewModel(cancelled.id).uiState.test {
                skipItems(1) // Loading
                assertEquals(ReceiptUiState.Cancelled(expectedReceipt(cancelled, withTransaction = false)), awaitItem())
            }
        }

    @Test
    fun `AC-19 - dado historico com varias compras quando abrir uma delas entao exibe o comprovante dela`() =
        runTest {
            val older =
                aPurchase(
                    reference = "a0b1c2d3-0001-4aaa-8bbb-cc00dd00ee01",
                    event = anEvent(name = "Noite de Jazz"),
                    status = PurchaseStatus.Approved,
                    transaction = aTransaction(),
                    createdAt = TEST_NOW.minus(1, ChronoUnit.DAYS),
                )
            val newer =
                aPurchase(
                    reference = "a0b1c2d3-0002-4aaa-8bbb-cc00dd00ee02",
                    event = anEvent(id = "e2", name = "Stand-up"),
                    status = PurchaseStatus.Cancelled,
                    createdAt = TEST_NOW,
                )
            repository.createPending(older)
            repository.createPending(newer) // the fake only rejects open purchases; both are results

            createViewModel(older.id).uiState.test {
                skipItems(1) // Loading
                assertEquals(ReceiptUiState.Approved(expectedReceipt(older, withTransaction = true)), awaitItem())
            }
        }

    @Test
    fun `dado compra ainda em andamento quando abrir o comprovante entao permanece carregando`() =
        runTest {
            val pending = aPurchase(status = PurchaseStatus.Pending)
            repository.createPending(pending)

            createViewModel(pending.id).uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `dado compra em recuperacao quando abrir o comprovante entao permanece carregando`() =
        runTest {
            val recovering = aPurchase(status = PurchaseStatus.Recovering)
            repository.createPending(recovering)

            createViewModel(recovering.id).uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `AC-27 - dado compra com falha tecnica quando abrir o comprovante entao exibe erro`() =
        runTest {
            val failed = aPurchase(status = PurchaseStatus.Failed)
            repository.createPending(failed)

            createViewModel(failed.id).uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                assertEquals(ReceiptUiState.Error, awaitItem())
            }
        }

    @Test
    fun `AC-27 - dado id sem compra registrada quando abrir o comprovante entao exibe erro`() =
        runTest {
            createViewModel("inexistente").uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                assertEquals(ReceiptUiState.Error, awaitItem())
            }
        }

    @Test
    fun `AC-27 - dado registro ilegivel quando tentar de novo entao volta a carregar e repete a leitura`() =
        runTest {
            val approved = aPurchase(status = PurchaseStatus.Approved, transaction = aTransaction())
            repository.createPending(approved)
            val unreadable = UnreadablePurchaseRepository(repository)
            val viewModel = createViewModel(approved.id, unreadable)

            viewModel.uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                assertEquals(ReceiptUiState.Error, awaitItem())

                viewModel.onEvent(ReceiptUiEvent.OnRetry)
                assertEquals(ReceiptUiState.Loading, awaitItem())
                assertEquals(ReceiptUiState.Error, awaitItem())

                unreadable.readable = true
                viewModel.onEvent(ReceiptUiEvent.OnRetry)
                assertEquals(ReceiptUiState.Loading, awaitItem())
                assertEquals(ReceiptUiState.Approved(expectedReceipt(approved, withTransaction = true)), awaitItem())
            }
        }

    @Test
    fun `dado comprovante exibido quando tentar de novo entao a leitura nao e repetida`() =
        runTest {
            val approved = aPurchase(status = PurchaseStatus.Approved, transaction = aTransaction())
            repository.createPending(approved)
            val viewModel = createViewModel(approved.id)

            viewModel.uiState.test {
                skipItems(1) // Loading
                val shown = awaitItem()

                viewModel.onEvent(ReceiptUiEvent.OnRetry)

                expectNoEvents()
                assertEquals(shown, viewModel.uiState.value)
            }
        }

    @Test
    fun `dado argumento de rota ausente quando criar o ViewModel entao e erro de programacao`() {
        assertThrows(IllegalStateException::class.java) {
            createViewModel(purchaseId = null)
        }
    }

    @Test
    fun `dado comprovante exibido quando tocar em nova venda entao o estado nao muda`() =
        runTest {
            val approved = aPurchase(status = PurchaseStatus.Approved, transaction = aTransaction())
            repository.createPending(approved)
            val viewModel = createViewModel(approved.id)

            viewModel.uiState.test {
                skipItems(1) // Loading
                val shown = awaitItem()
                viewModel.onEvent(ReceiptUiEvent.OnNewSale)
                expectNoEvents()
                assertEquals(shown, viewModel.uiState.value)
            }
        }

    private fun expectedReceipt(
        purchase: Purchase,
        withTransaction: Boolean,
    ) = ReceiptData(
        purchaseNumber = purchase.reference.value,
        eventName = purchase.eventName,
        eventStartsAt = purchase.eventStartsAt,
        venue = purchase.venue,
        quantity = purchase.quantity,
        unitPrice = purchase.unitPrice,
        total = purchase.total,
        purchasedAt = purchase.createdAt,
        transaction =
            if (withTransaction) {
                ReceiptTransaction(
                    brand = "Visa",
                    authCode = "A1B2C3",
                    nsu = "000123456",
                )
            } else {
                null
            },
    )

    /** Fails to read any purchase until [readable] is set, like a storage error that goes away (BR-28). */
    private class UnreadablePurchaseRepository(
        private val delegate: FakePurchaseRepository,
    ) : PurchaseRepository by delegate {
        var readable: Boolean = false

        override fun observePurchase(id: String): Flow<DataResult<Purchase>> =
            if (readable) {
                delegate.observePurchase(id)
            } else {
                flowOf(DataResult.Failure(TicketingError.StorageFailure))
            }
    }
}
