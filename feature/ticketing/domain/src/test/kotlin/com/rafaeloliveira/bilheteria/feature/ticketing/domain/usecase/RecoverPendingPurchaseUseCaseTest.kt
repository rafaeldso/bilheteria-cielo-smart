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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Clock
import java.time.ZoneOffset

class RecoverPendingPurchaseUseCaseTest {
    private val now = TEST_NOW.plusSeconds(30)
    private val gateway = FakePaymentGateway()
    private val useCaseFor = { repository: FakePurchaseRepository ->
        RecoverPendingPurchaseUseCase(repository, gateway, Clock.fixed(now, ZoneOffset.UTC), PurchaseWriteLock())
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseStatus::class, names = ["Pending", "Recovering"])
    fun `AC-15 - dado compra aberta ao reabrir quando recuperar entao fica em recuperacao e consulta a pagina 0`(
        status: PurchaseStatus,
    ) = runTest {
        val repository = FakePurchaseRepository(listOf(aPurchase(status = status)))

        val result = useCaseFor(repository)()

        val expected = aPurchase(status = PurchaseStatus.Recovering).copy(updatedAt = now)
        assertEquals(DataResult.Success(true), result)
        assertEquals(listOf(expected), repository.all)
        assertEquals(listOf(0), gateway.lookupRequests)
        assertEquals(0, gateway.finished)
    }

    @Test
    fun `AC-14 - dado resposta ilegivel quando recuperar entao a compra nunca fica aprovada nem falhou`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            useCaseFor(repository)()

            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
            assertEquals(null, repository.all.single().transaction)
            assertEquals(null, repository.all.single().failureReason)
        }

    @Test
    fun `AC-15 - dado nenhuma compra aberta quando recuperar entao nao consulta a Cielo`() =
        runTest {
            val closed = aPurchase(status = PurchaseStatus.Approved)
            val repository = FakePurchaseRepository(listOf(closed))

            val result = useCaseFor(repository)()

            assertEquals(DataResult.Success(false), result)
            assertEquals(listOf(closed), repository.all)
            assertTrue(gateway.lookupRequests.isEmpty())
        }

    @Test
    fun `AC-15 - dado app Cielo ausente quando recuperar entao falha sem cobranca e encerra a sessao`() =
        runTest {
            gateway.terminalAvailable = false
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCaseFor(repository)()

            val expected =
                aPurchase(status = PurchaseStatus.Failed, failureReason = FailureReason.TerminalAppUnavailable)
                    .copy(updatedAt = now)
            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-15 - dado falha de armazenamento quando recuperar entao propaga a falha sem consultar a Cielo`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            repository.failStorage = true

            val result = useCaseFor(repository)()

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
            assertTrue(gateway.lookupRequests.isEmpty())
        }

    @Test
    fun `BR-15 - dado escrita em andamento sob a trava quando recuperar entao espera a trava`() =
        runTest {
            val lock = PurchaseWriteLock()
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            val useCase = RecoverPendingPurchaseUseCase(repository, gateway, Clock.fixed(now, ZoneOffset.UTC), lock)
            val release = CompletableDeferred<Unit>()
            launch { lock.withLock { release.await() } }
            advanceUntilIdle()

            launch { useCase() }
            advanceUntilIdle()
            assertEquals(PurchaseStatus.Pending, repository.all.single().status)

            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
        }
}
