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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.PurchaseRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.ReceiptRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase.PurchaseUiState
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase.PurchaseViewModelTestBase
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptUiState
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * The type-safe navigation fills the `SavedStateHandle` with one entry per route property, named after it. These
 * tests build the handle from the property names of the routes in `:feature:ticketing:api` and check the ViewModels
 * read them: renaming a property or a key on one side only makes them fail (plan §4).
 */
internal class RouteArgumentKeysTest : PurchaseViewModelTestBase() {
    private fun key(descriptor: kotlinx.serialization.descriptors.SerialDescriptor): String {
        assertEquals(1, descriptor.elementsCount)
        return descriptor.getElementName(0)
    }

    @Test
    fun `dado a chave da rota de compra quando abrir a tela entao a compra carrega o evento da rota`() =
        runTest {
            val handle = SavedStateHandle(mapOf(key(PurchaseRoute.serializer().descriptor) to event.id))
            val viewModel = createViewModel(repositoryWith(), handle)

            viewModel.uiState.test {
                assertEquals(PurchaseUiState.Loading, awaitItem())
                advanceUntilIdle()
                val state = awaitItem()
                assertTrue(state is PurchaseUiState.Editing)
                assertEquals(event.id, (state as PurchaseUiState.Editing).event.id)
            }
        }
}

class ReceiptRouteArgumentKeyTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun `dado a chave da rota do comprovante quando abrir a tela entao o comprovante carrega a compra da rota`() =
        runTest {
            val cancelled = aPurchase(reference = "route-purchase", status = PurchaseStatus.Cancelled)
            val descriptor = ReceiptRoute.serializer().descriptor
            assertEquals(1, descriptor.elementsCount)
            val handle = SavedStateHandle(mapOf(descriptor.getElementName(0) to cancelled.id))
            val viewModel = ReceiptViewModel(handle, ObservePurchaseUseCase(FakePurchaseRepository(listOf(cancelled))))

            viewModel.uiState.test {
                assertEquals(ReceiptUiState.Loading, awaitItem())
                assertTrue(awaitItem() is ReceiptUiState.Cancelled)
            }
        }
}
