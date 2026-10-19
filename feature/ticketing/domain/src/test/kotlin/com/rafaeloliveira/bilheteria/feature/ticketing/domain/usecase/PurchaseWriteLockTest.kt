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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
