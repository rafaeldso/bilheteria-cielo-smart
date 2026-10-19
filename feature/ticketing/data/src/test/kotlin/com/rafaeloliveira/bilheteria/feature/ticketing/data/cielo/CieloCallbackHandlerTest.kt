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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo

import com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase.RecordingLogger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.HandlePaymentOutcomeUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.PurchaseWriteLock
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.RecoverPendingPurchaseUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.ZoneOffset

class CieloCallbackHandlerTest {
    private val clock = Clock.fixed(TEST_NOW.plusSeconds(30), ZoneOffset.UTC)
    private val writeLock = PurchaseWriteLock()
    private val gateway = FakePaymentGateway()
    private val logger = RecordingLogger()
    private val reference = "0b8f2d1c-1111-4222-8333-444455556666" // the one in payment_approved.json (10000 cents)

    private fun handler(
        repository: FakePurchaseRepository,
        scope: TestScope,
    ) = CieloCallbackHandler(
        parser = CieloResponseParser(),
        handlePaymentOutcome =
            HandlePaymentOutcomeUseCase(
                repository = repository,
                gateway = gateway,
                recover = RecoverPendingPurchaseUseCase(repository, gateway, clock, writeLock),
                clock = clock,
                logger = logger,
                lock = writeLock,
            ),
        scope = scope,
        logger = logger,
    )

    @Test
    fun `AC-09 - dado retorno autorizado quando o callback chegar entao registra aprovada e avisa que terminou`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase(reference = reference)))
            var done = false

            handler(repository, this).handle(cieloBase64(fixture("payment_approved.json"))) { done = true }
            testScheduler.advanceUntilIdle()

            assertEquals(PurchaseStatus.Approved, repository.all.single().status)
            assertTrue(done)
        }

    @Test
    fun `AC-14 - dado resposta ilegivel quando o callback chegar entao consulta a Cielo antes de cobrar`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase(reference = reference)))
            var done = false

            handler(repository, this).handle("isto-nao-e-base64-nem-json") { done = true }
            testScheduler.advanceUntilIdle()

            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
            assertEquals(listOf(0), gateway.lookupRequests)
            assertTrue(done)
        }

    @Test
    fun `AC-14 - dado callback sem o parametro response quando chegar entao trata como ilegivel`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase(reference = reference)))

            handler(repository, this).handle(null) {}
            testScheduler.advanceUntilIdle()

            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
        }

    @Test
    fun `AC-09 - dado retorno de outra reference quando o callback chegar entao nao altera a compra aberta`() =
        runTest {
            val other = aPurchase(reference = "ffffffff-0000-4000-8000-000000000000")
            val repository = FakePurchaseRepository(listOf(other))

            handler(repository, this).handle(cieloBase64(fixture("payment_approved.json"))) {}
            testScheduler.advanceUntilIdle()

            assertEquals(PurchaseStatus.Pending, repository.all.single().status)
        }

    @Test
    fun `AC-09 - dado falha no armazenamento quando o callback chegar entao termina e loga sem a resposta`() =
        runTest {
            val repository =
                FakePurchaseRepository(
                    listOf(aPurchase(reference = reference)),
                ).apply { failStorage = true }
            var done = false
            val response = cieloBase64(fixture("payment_approved.json"))

            handler(repository, this).handle(response) { done = true }
            testScheduler.advanceUntilIdle()

            assertTrue(done)
            assertEquals(listOf("payment_outcome_not_applied"), logger.errors)
        }
}
