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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation

import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveOpenPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase.UnreadableOpenPurchaseRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class OpenPurchaseRedirectViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun createViewModel(repository: PurchaseRepository) =
        OpenPurchaseRedirectViewModel(ObserveOpenPurchaseUseCase(repository))

    @Test
    fun `AC-15 - dado compra em andamento sem resultado quando o app abrir entao leva a compra do evento`() =
        runTest {
            val pending = aPurchase(reference = "open", status = PurchaseStatus.Pending)
            val viewModel = createViewModel(FakePurchaseRepository(listOf(pending)))

            viewModel.uiState.test {
                assertEquals(OpenPurchaseRedirectUiState.Checking, awaitItem())
                assertEquals(OpenPurchaseRedirectUiState.ToPurchase(pending.eventId), awaitItem())
            }
        }

    @Test
    fun `AC-15 - dado compra em recuperacao quando o app abrir entao leva a compra do evento`() =
        runTest {
            val recovering = aPurchase(reference = "open", status = PurchaseStatus.Recovering)
            val viewModel = createViewModel(FakePurchaseRepository(listOf(recovering)))

            viewModel.uiState.test {
                assertEquals(OpenPurchaseRedirectUiState.Checking, awaitItem())
                assertEquals(OpenPurchaseRedirectUiState.ToPurchase(recovering.eventId), awaitItem())
            }
        }

    @Test
    fun `dado nenhuma compra aberta quando o app abrir entao fica na lista de eventos`() =
        runTest {
            val approved = aPurchase(reference = "done", status = PurchaseStatus.Approved)
            val viewModel = createViewModel(FakePurchaseRepository(listOf(approved)))

            viewModel.uiState.test {
                assertEquals(OpenPurchaseRedirectUiState.Checking, awaitItem())
                assertEquals(OpenPurchaseRedirectUiState.None, awaitItem())
            }
        }

    @Test
    fun `dado compra aberta ilegivel quando o app abrir entao nao redireciona e a tela de compra trata a falha`() =
        runTest {
            val viewModel = createViewModel(UnreadableOpenPurchaseRepository(FakePurchaseRepository()))

            viewModel.uiState.test {
                assertEquals(OpenPurchaseRedirectUiState.Checking, awaitItem())
                assertEquals(OpenPurchaseRedirectUiState.None, awaitItem())
            }
        }

    @Test
    fun `dado redirecionamento aplicado quando consumir entao nao repete`() =
        runTest {
            val pending = aPurchase(reference = "open", status = PurchaseStatus.Pending)
            val viewModel = createViewModel(FakePurchaseRepository(listOf(pending)))

            viewModel.uiState.test {
                assertEquals(OpenPurchaseRedirectUiState.Checking, awaitItem())
                assertEquals(OpenPurchaseRedirectUiState.ToPurchase(pending.eventId), awaitItem())
                viewModel.onEvent(OpenPurchaseRedirectUiEvent.OnRedirectHandled)
                assertEquals(OpenPurchaseRedirectUiState.None, awaitItem())
            }
        }
}
