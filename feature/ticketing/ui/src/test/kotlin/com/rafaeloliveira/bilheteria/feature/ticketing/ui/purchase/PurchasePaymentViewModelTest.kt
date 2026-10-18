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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PurchasePaymentViewModelTest : PurchaseViewModelTestBase() {
    @Test
    fun `AC-07 - dado quantidade 2 quando pagar entao grava pendente antes de abrir a Cielo`() =
        runTest {
            val repository = repositoryWith()
            val inspecting = InspectingGateway(gateway, repository)
            val viewModel = createViewModel(repository, paymentGateway = inspecting)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnIncrement)
                viewModel.onEvent(PurchaseUiEvent.OnPay)

                val state = latest(viewModel)

                val sent = gateway.paymentRequests.single()
                assertEquals(FIRST_REFERENCE, sent.reference.value)
                assertEquals(2, sent.quantity)
                assertEquals(Money(2 * UNIT_PRICE_CENTS), sent.total)
                // At the moment the terminal was asked to charge, the record was already stored as Pending.
                assertEquals(listOf(FIRST_REFERENCE to PurchaseStatus.Pending), inspecting.storedAtRequest)
                assertEquals(PurchaseStatus.Pending, repository.all.single().status)
                assertEquals(PurchaseUiState.AwaitingPayment(summary(null), 2, Money(2 * UNIT_PRICE_CENTS)), state)
            }
        }

    @Test
    fun `AC-08 - dado compra em andamento quando pagar de novo entao nao abre segundo pagamento`() =
        runTest {
            val viewModel = createViewModel(repositoryWith())
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)
                viewModel.onEvent(PurchaseUiEvent.OnPay)
                advanceUntilIdle()
                viewModel.onEvent(PurchaseUiEvent.OnPay)

                val state = latest(viewModel)

                assertEquals(1, gateway.paymentRequests.size)
                assertTrue(state is PurchaseUiState.AwaitingPayment)
            }
        }

    @Test
    fun `AC-08 - dado compra em andamento quando a tela for recriada e pagar entao nao abre pagamento`() =
        runTest {
            val repository = repositoryWith(aPurchase(reference = FIRST_REFERENCE, event = event, quantity = 2))
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)

                val state = latest(viewModel)

                assertTrue(gateway.paymentRequests.isEmpty())
                assertEquals(PurchaseUiState.Recovering(summary(null), 2, Money(2 * UNIT_PRICE_CENTS)), state)
            }
        }

    @Test
    fun `AC-12 - dado app Cielo ausente quando pagar entao erro sem cobranca`() =
        runTest {
            gateway.terminalAvailable = false
            val repository = repositoryWith()
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)

                val state = latest(viewModel)

                val total = Money(UNIT_PRICE_CENTS)
                val expected = PurchaseUiState.Error(summary(), 1, total, PurchaseErrorReason.NoCieloApp)
                assertEquals(expected, state)
                assertTrue(gateway.paymentRequests.isEmpty())
                assertEquals(FailureReason.TerminalAppUnavailable, repository.all.single().failureReason)
            }
        }

    @Test
    fun `dado falha ao gravar a compra quando pagar entao erro generico e pagamento nao aberto`() =
        runTest {
            val repository = repositoryWith()
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                repository.failStorage = true
                viewModel.onEvent(PurchaseUiEvent.OnPay)

                val state = latest(viewModel)

                assertEquals(PurchaseErrorReason.Generic, (state as PurchaseUiState.Error).reason)
                assertTrue(gateway.paymentRequests.isEmpty())
            }
        }

    @Test
    fun `AC-13 - dado erro generico quando tentar de novo entao nova referencia sem duplicar`() =
        runTest {
            failedPaymentThenRetry(FailureReason.Generic, PurchaseErrorReason.Generic)
        }

    @Test
    fun `AC-13 - dado erro de autenticacao quando tentar de novo entao nova referencia sem duplicar`() =
        runTest {
            failedPaymentThenRetry(FailureReason.Authentication, PurchaseErrorReason.Authentication)
        }

    private suspend fun TestScope.failedPaymentThenRetry(
        failure: FailureReason,
        expectedReason: PurchaseErrorReason,
    ) {
        val repository = repositoryWith()
        val viewModel = createViewModel(repository)
        observing(viewModel) {
            latest(viewModel)
            viewModel.onEvent(PurchaseUiEvent.OnIncrement)
            viewModel.onEvent(PurchaseUiEvent.OnPay)
            latest(viewModel)

            outcomes(repository)(PaymentOutcome.Failed(PurchaseReference(FIRST_REFERENCE), failure))
            val error = latest(viewModel)

            val total = Money(2 * UNIT_PRICE_CENTS)
            assertEquals(PurchaseUiState.Error(summary(null), 2, total, expectedReason), error)
            assertEquals(PurchaseStatus.Failed, repository.all.single().status)

            viewModel.onEvent(PurchaseUiEvent.OnRetry)
            val retried = latest(viewModel)

            assertEquals(PurchaseUiState.AwaitingPayment(summary(null), 2, total), retried)
            assertEquals(listOf(FIRST_REFERENCE, SECOND_REFERENCE), gateway.paymentRequests.map { it.reference.value })
            assertEquals(1, repository.all.count { it.status.isOpen })
        }
    }

    /** Records which purchases were stored when the terminal was asked to charge (reference and status). */
    private class InspectingGateway(
        private val delegate: FakePaymentGateway,
        private val repository: FakePurchaseRepository,
    ) : PaymentGateway by delegate {
        var storedAtRequest: List<Pair<String, PurchaseStatus>> = emptyList()
            private set

        override suspend fun requestPayment(purchase: Purchase): DataResult<Unit> {
            storedAtRequest = repository.all.map { it.reference.value to it.status }
            return delegate.requestPayment(purchase)
        }
    }
}
