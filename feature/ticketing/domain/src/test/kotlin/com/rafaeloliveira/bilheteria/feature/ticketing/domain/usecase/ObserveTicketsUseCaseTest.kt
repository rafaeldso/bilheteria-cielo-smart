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

import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeQrEncoder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ObserveTicketsUseCaseTest {
    private val encoder = FakeQrEncoder()

    private fun TestScope.useCase(repository: FakePurchaseRepository) =
        ObserveTicketsUseCase(repository, GenerateTicketsUseCase(), encoder, StandardTestDispatcher(testScheduler))

    @Test
    fun `AC-29 - dado compra aprovada de 3 quando observar entao traz um QR Code por ingresso`() =
        runTest {
            val purchase = aPurchase(quantity = 3, status = PurchaseStatus.Approved)

            val sheet =
                (
                    useCase(
                        FakePurchaseRepository(listOf(purchase)),
                    )(purchase.id).first() as DataResult.Success
                ).value

            assertEquals(3, sheet.items.size)
            assertEquals(3, encoder.encoded.size)
            assertEquals(purchase.eventName, sheet.set.eventName)
        }

    @Test
    fun `AC-31 - dado a mesma compra quando observar de novo entao os mesmos conteudos sao codificados`() =
        runTest {
            val purchase = aPurchase(quantity = 2, status = PurchaseStatus.Approved)
            val repository = FakePurchaseRepository(listOf(purchase))

            useCase(repository)(purchase.id).first()
            val firstRun = encoder.encoded.toList()
            useCase(repository)(purchase.id).first()

            assertEquals(firstRun + firstRun, encoder.encoded)
        }

    @ParameterizedTest
    @EnumSource(PurchaseStatus::class, names = ["Approved"], mode = EnumSource.Mode.EXCLUDE)
    fun `AC-36 - dado compra que nao esta aprovada quando observar entao erro e nenhum QR Code`(
        status: PurchaseStatus,
    ) = runTest {
        val purchase = aPurchase(status = status)

        val result = useCase(FakePurchaseRepository(listOf(purchase)))(purchase.id).first()

        assertEquals(DataResult.Failure(TicketingError.TicketsUnavailable), result)
        assertEquals(0, encoder.encoded.size)
    }

    @Test
    fun `AC-36 - dado compra inexistente quando observar entao erro sem QR Code`() =
        runTest {
            val result = useCase(FakePurchaseRepository())("nao-existe").first()

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
            assertEquals(0, encoder.encoded.size)
        }

    @Test
    fun `AC-35 - dado que a geracao do QR Code falha quando observar entao erro sem nenhum ingresso`() =
        runTest {
            val purchase = aPurchase(quantity = 2, status = PurchaseStatus.Approved)
            encoder.fail = true

            val result = useCase(FakePurchaseRepository(listOf(purchase)))(purchase.id).first()

            assertEquals(DataResult.Failure(TicketingError.TicketGenerationFailed), result)
        }

    @Test
    fun `AC-37 - dado compra falhou por nao encontrado quando aprovada depois entao passa a oferecer os ingressos`() =
        runTest {
            val failed =
                aPurchase(quantity = 2, status = PurchaseStatus.Failed, failureReason = FailureReason.NotFound)
            val repository = FakePurchaseRepository(listOf(failed))

            useCase(repository)(failed.id).test {
                assertEquals(DataResult.Failure(TicketingError.TicketsUnavailable), awaitItem())
                repository.update(failed.copy(status = PurchaseStatus.Approved, failureReason = null))
                val sheet = (awaitItem() as DataResult.Success).value
                assertEquals(2, sheet.items.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
