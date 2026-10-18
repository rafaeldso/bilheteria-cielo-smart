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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveOpenPurchaseUseCaseTest {
    @Test
    fun `AC-08 - dado compra pendente quando observar entao emite a compra aberta`() =
        runTest {
            val pending = aPurchase(status = PurchaseStatus.Pending)
            val useCase = ObserveOpenPurchaseUseCase(FakePurchaseRepository(listOf(pending)))

            assertEquals(DataResult.Success(pending), useCase().first())
        }

    @Test
    fun `AC-15 - dado compra em recuperacao quando observar entao emite a compra aberta`() =
        runTest {
            val recovering = aPurchase(status = PurchaseStatus.Recovering)
            val useCase = ObserveOpenPurchaseUseCase(FakePurchaseRepository(listOf(recovering)))

            assertEquals(DataResult.Success(recovering), useCase().first())
        }

    @Test
    fun `AC-15 - dado so compras finalizadas quando observar entao emite nulo`() =
        runTest {
            val finished =
                listOf(
                    aPurchase(reference = "ref-1", status = PurchaseStatus.Approved),
                    aPurchase(reference = "ref-2", status = PurchaseStatus.Failed),
                )
            val useCase = ObserveOpenPurchaseUseCase(FakePurchaseRepository(finished))

            assertEquals(DataResult.Success(null), useCase().first())
        }
}
