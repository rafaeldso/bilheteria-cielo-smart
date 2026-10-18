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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class ObservePurchaseHistoryUseCaseTest {
    private fun purchase(
        reference: String,
        status: PurchaseStatus,
        ageMinutes: Long,
    ) = aPurchase(
        reference = reference,
        status = status,
        createdAt = TEST_NOW.minus(Duration.ofMinutes(ageMinutes)),
    )

    private fun idsOf(result: DataResult<List<Purchase>>): List<String> =
        (result as DataResult.Success).value.map { it.id }

    @Test
    fun `AC-18 - dado vendas em datas diferentes quando observar historico entao ordena da mais recente`() =
        runTest {
            val repository =
                FakePurchaseRepository(
                    listOf(
                        purchase("old", PurchaseStatus.Approved, ageMinutes = 30),
                        purchase("new", PurchaseStatus.Cancelled, ageMinutes = 1),
                        purchase("mid", PurchaseStatus.Declined, ageMinutes = 10),
                    ),
                )

            val result = ObservePurchaseHistoryUseCase(repository)().first()

            assertEquals(listOf("new", "mid", "old"), idsOf(result))
        }

    @Test
    fun `AC-18 - dado tentativas Failed Pending e Recovering quando observar historico entao ficam fora`() =
        runTest {
            val repository =
                FakePurchaseRepository(
                    listOf(
                        purchase("approved", PurchaseStatus.Approved, ageMinutes = 5),
                        purchase("failed", PurchaseStatus.Failed, ageMinutes = 4),
                        purchase("recovering", PurchaseStatus.Recovering, ageMinutes = 3),
                    ),
                )

            val result = ObservePurchaseHistoryUseCase(repository)().first()

            assertEquals(listOf("approved"), idsOf(result))
        }

    @Test
    fun `AC-18 - dado repositorio fora de ordem e com nao vendas quando observar entao o domain aplica a regra`() =
        runTest {
            val unordered =
                listOf(
                    purchase("old", PurchaseStatus.Approved, ageMinutes = 60),
                    purchase("pending", PurchaseStatus.Pending, ageMinutes = 1),
                    purchase("new", PurchaseStatus.Declined, ageMinutes = 2),
                    purchase("failed", PurchaseStatus.Failed, ageMinutes = 3),
                    purchase("mid", PurchaseStatus.Cancelled, ageMinutes = 20),
                    purchase("recovering", PurchaseStatus.Recovering, ageMinutes = 4),
                )

            val result = ObservePurchaseHistoryUseCase(UnfilteredHistoryRepository(unordered))().first()

            assertEquals(listOf("new", "mid", "old"), idsOf(result))
        }

    @Test
    fun `AC-20 - dado que nao ha compras quando observar historico entao devolve Success com lista vazia`() =
        runTest {
            val result = ObservePurchaseHistoryUseCase(FakePurchaseRepository())().first()

            assertEquals(DataResult.Success(emptyList<Purchase>()), result)
        }

    @Test
    fun `AC-21 - dado falha de leitura quando observar historico entao propaga Failure StorageFailure`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(purchase("a", PurchaseStatus.Approved, ageMinutes = 1)))
            repository.failStorage = true

            val result = ObservePurchaseHistoryUseCase(repository)().first()

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
        }

    /** Emits the given list untouched, to prove the BR-18 rule lives in the use case. */
    private class UnfilteredHistoryRepository(
        private val history: List<Purchase>,
    ) : PurchaseRepository {
        override fun observeHistory(): Flow<DataResult<List<Purchase>>> = flowOf(DataResult.Success(history))

        override fun observePurchase(id: String): Flow<DataResult<Purchase>> = error("unused")

        override fun observeOpenPurchase(): Flow<DataResult<Purchase?>> = error("unused")

        override fun observeApprovedQuantities(): Flow<DataResult<Map<String, Int>>> = error("unused")

        override suspend fun createPending(purchase: Purchase): DataResult<Unit> = error("unused")

        override suspend fun update(purchase: Purchase): DataResult<Unit> = error("unused")

        override suspend fun findOpen(): DataResult<Purchase?> = error("unused")
    }
}
