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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObservePurchaseUseCaseTest {
    private fun purchaseOf(result: DataResult<Purchase>): Purchase = (result as DataResult.Success).value

    @Test
    fun `AC-16 - dado compra aprovada quando observar entao traz bandeira autorizacao e NSU`() =
        runTest {
            val approved = aPurchase(status = PurchaseStatus.Approved, transaction = aTransaction())
            val useCase = ObservePurchaseUseCase(FakePurchaseRepository(listOf(approved)))

            val transaction = purchaseOf(useCase(approved.id).first()).transaction

            assertEquals("Visa", transaction?.brand)
            assertEquals("A1B2C3", transaction?.authCode)
            assertEquals("000123456", transaction?.nsu)
        }

    @Test
    fun `AC-16 - dado compra negada ou cancelada quando observar entao nao tem transaction`() =
        runTest {
            val declined = aPurchase(reference = "ref-declined", status = PurchaseStatus.Declined)
            val cancelled = aPurchase(reference = "ref-cancelled", status = PurchaseStatus.Cancelled)
            val useCase = ObservePurchaseUseCase(FakePurchaseRepository(listOf(declined, cancelled)))

            assertNull(purchaseOf(useCase(declined.id).first()).transaction)
            assertNull(purchaseOf(useCase(cancelled.id).first()).transaction)
        }

    @Test
    fun `AC-16 - dado o modelo da comprovante quando inspecionar campos entao nao ha dado de cartao`() {
        val fields =
            (Purchase::class.java.declaredFields + TransactionData::class.java.declaredFields)
                .map { it.name.lowercase() }

        assertTrue(fields.none { it == "pan" || it == "cvv" || "card" in it || "track" in it })
    }

    @Test
    fun `AC-19 - dado varias compras quando observar pelo id entao devolve a compra certa`() =
        runTest {
            val first = aPurchase(reference = "ref-1", status = PurchaseStatus.Approved)
            val second = aPurchase(reference = "ref-2", status = PurchaseStatus.Declined)
            val useCase = ObservePurchaseUseCase(FakePurchaseRepository(listOf(first, second)))

            assertEquals(second, purchaseOf(useCase("ref-2").first()))
            assertEquals(first, purchaseOf(useCase("ref-1").first()))
        }

    @Test
    fun `AC-21 - dado compra nao encontrada quando observar entao propaga Failure StorageFailure`() =
        runTest {
            val useCase = ObservePurchaseUseCase(FakePurchaseRepository())

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), useCase("missing").first())
        }
}
