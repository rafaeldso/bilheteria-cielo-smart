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
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.LookupOrder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeEventCatalogRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FixedReferenceGenerator
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.HandlePaymentOutcomeUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveAvailableEventsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveOpenPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.RecoverPendingPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.StartPurchaseUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.ZoneOffset

internal class PurchaseRecoveryViewModelTest : PurchaseViewModelTestBase() {
    @Test
    fun `AC-14 - dado resposta ilegivel quando processar entao confirma com a Cielo antes de cobrar`() =
        runTest {
            val repository = repositoryWith()
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)
                latest(viewModel)

                outcomes(repository)(PaymentOutcome.Unreadable)
                val state = latest(viewModel)

                assertEquals(PurchaseUiState.Recovering(summary(null), 1, Money(UNIT_PRICE_CENTS)), state)
                assertEquals(listOf(0), gateway.lookupRequests)
                assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
            }
        }

    @Test
    fun `AC-15 - dado compra aberta ao reabrir quando consultar a Cielo entao exibe Recovering e pede a consulta`() =
        runTest {
            val repository = repositoryWith(aPurchase(reference = FIRST_REFERENCE, event = event, quantity = 2))
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                val state = latest(viewModel)

                assertEquals(PurchaseUiState.Recovering(summary(null), 2, Money(2 * UNIT_PRICE_CENTS)), state)
                assertEquals(listOf(0), gateway.lookupRequests)
                assertTrue(gateway.paymentRequests.isEmpty())
            }
        }

    @Test
    fun `AC-15 - dado pedido nao encontrado quando consultar entao volta a compra e permite tentar`() =
        runTest {
            val repository = repositoryWith(aPurchase(reference = FIRST_REFERENCE, event = event, quantity = 2))
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)

                outcomes(repository)(PaymentOutcome.LookupPage(page = 0, totalPages = 1, orders = emptyList()))
                val state = latest(viewModel)

                val total = Money(2 * UNIT_PRICE_CENTS)
                val notFound = PurchaseErrorReason.RecoveryNotFound
                assertEquals(PurchaseUiState.Error(summary(null), 2, total, notFound), state)
                assertEquals(FailureReason.NotFound, repository.all.single().failureReason)

                viewModel.onEvent(PurchaseUiEvent.OnRetry)

                assertEquals(PurchaseUiState.AwaitingPayment(summary(null), 2, total), latest(viewModel))
                assertEquals(2, gateway.paymentRequests.single().quantity)
            }
        }

    @Test
    fun `AC-15 - dado pedido encontrado aprovado quando consultar entao abre o comprovante`() =
        runTest {
            val pending = aPurchase(reference = FIRST_REFERENCE, event = event, quantity = 2)
            val repository = repositoryWith(pending)
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                val approved = PaymentOutcome.Approved(pending.reference, pending.total, aTransaction())

                outcomes(repository)(PaymentOutcome.LookupPage(0, 1, listOf(LookupOrder(pending.reference, approved))))

                assertEquals(PurchaseUiState.Completed(FIRST_REFERENCE), latest(viewModel))
            }
        }

    @Test
    fun `AC-09 - dado compra em andamento quando a Cielo aprova entao sinaliza o comprovante`() =
        runTest {
            val repository = repositoryWith()
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)
                latest(viewModel)
                val reference = PurchaseReference(FIRST_REFERENCE)

                outcomes(repository)(PaymentOutcome.Approved(reference, Money(UNIT_PRICE_CENTS), aTransaction()))

                assertEquals(PurchaseUiState.Completed(FIRST_REFERENCE), latest(viewModel))
            }
        }

    @Test
    fun `AC-10 - dado compra em andamento quando a Cielo cancela entao sinaliza o comprovante`() =
        runTest {
            val repository = repositoryWith()
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)
                latest(viewModel)

                outcomes(repository)(PaymentOutcome.Cancelled(PurchaseReference(FIRST_REFERENCE)))

                assertEquals(PurchaseUiState.Completed(FIRST_REFERENCE), latest(viewModel))
            }
        }

    @Test
    fun `AC-11 - dado compra em andamento quando a Cielo nega entao sinaliza o comprovante`() =
        runTest {
            val repository = repositoryWith()
            val viewModel = createViewModel(repository)
            observing(viewModel) {
                latest(viewModel)
                viewModel.onEvent(PurchaseUiEvent.OnPay)
                latest(viewModel)

                outcomes(repository)(PaymentOutcome.Declined(PurchaseReference(FIRST_REFERENCE)))

                assertEquals(PurchaseUiState.Completed(FIRST_REFERENCE), latest(viewModel))
            }
        }
}
