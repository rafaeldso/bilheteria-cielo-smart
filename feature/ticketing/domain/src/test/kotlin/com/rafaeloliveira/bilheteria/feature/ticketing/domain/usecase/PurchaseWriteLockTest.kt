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

import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.ZoneOffset

/** [PurchaseWriteLock] keeps the callback and the recovery from interleaving their updates (BR-29). */
class PurchaseWriteLockTest {
    @Test
    fun `BR-29 - dado sequencia em andamento quando outra comecar entao espera`() =
        runTest {
            val lock = PurchaseWriteLock()
            val release = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()

            launch {
                lock.withLock {
                    events += "first-start"
                    release.await()
                    events += "first-end"
                }
            }
            advanceUntilIdle()
            launch { lock.withLock { events += "second" } }
            advanceUntilIdle()
            assertEquals(listOf("first-start"), events)

            release.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("first-start", "first-end", "second"), events)
        }

    @Test
    fun `BR-29 - dado escrita em andamento sob a trava quando chegar o resultado entao espera a trava`() =
        runTest {
            val lock = PurchaseWriteLock()
            val gateway = FakePaymentGateway()
            val clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            val useCase =
                HandlePaymentOutcomeUseCase(
                    repository,
                    gateway,
                    RecoverPendingPurchaseUseCase(repository, gateway, clock, lock),
                    clock,
                    SilentLogger,
                    lock,
                )
            val approved = PaymentOutcome.Approved(aPurchase().reference, aPurchase().total, aTransaction())
            val release = CompletableDeferred<Unit>()
            launch { lock.withLock { release.await() } }
            advanceUntilIdle()

            launch { useCase(approved) }
            advanceUntilIdle()
            assertEquals(PurchaseStatus.Pending, repository.all.single().status)

            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(PurchaseStatus.Approved, repository.all.single().status)
        }

    private object SilentLogger : Logger {
        override fun info(message: String) = Unit

        override fun warn(
            message: String,
            throwable: Throwable?,
        ) = Unit

        override fun error(
            message: String,
            throwable: Throwable?,
        ) = Unit
    }
}
