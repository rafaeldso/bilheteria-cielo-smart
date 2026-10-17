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

import kotlin.coroutines.cancellation.CancellationException

/** Result type shared by every layer. Technical exceptions never cross the data layer. */
sealed interface DataResult<out T> {
    data class Success<T>(
        val value: T,
    ) : DataResult<T>

    data class Failure(
        val error: AppError,
    ) : DataResult<Nothing>
}

/** Cross-cutting errors. Features extend with their own sealed errors implementing [AppError]. */
interface AppError {
    data object Unknown : AppError
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> =
    when (this) {
        is DataResult.Success -> DataResult.Success(transform(value))
        is DataResult.Failure -> this
    }

/** Like runCatching, but never swallows coroutine cancellation. */
suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (expected: Exception) {
        Result.failure(expected)
    }
