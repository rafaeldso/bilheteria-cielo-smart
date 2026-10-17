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
package com.rafaeloliveira.bilheteria.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DataResultTest {
    @Test
    fun `map transforma apenas o sucesso`() {
        val success: DataResult<Int> = DataResult.Success(2)
        val failure: DataResult<Int> = DataResult.Failure(AppError.Unknown)

        assertEquals(DataResult.Success(4), success.map { it * 2 })
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `suspendRunCatching captura excecao comum`() =
        runTest {
            val result = suspendRunCatching { error("falha") }

            assertTrue(result.isFailure)
        }

    @Test
    fun `suspendRunCatching nunca engole cancelamento`() =
        runTest {
            assertThrows<CancellationException> {
                suspendRunCatching { throw CancellationException("cancelado") }
            }
        }
}
