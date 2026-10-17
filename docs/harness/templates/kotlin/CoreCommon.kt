// Module: core:common (Kotlin JVM, no Android dependencies)
package com.example.app.core.common

import javax.inject.Qualifier
import kotlin.coroutines.cancellation.CancellationException

/** Result type shared by every layer. Technical exceptions never cross the data layer. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>
}

/** Cross-cutting errors. Features extend with their own sealed errors implementing [AppError]. */
interface AppError {
    sealed interface Network : AppError {
        data object Offline : Network
        data object Timeout : Network
    }

    sealed interface Auth : AppError {
        data object SessionExpired : Auth
    }

    data object Unknown : AppError
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}

/** Like runCatching, but never swallows coroutine cancellation. */
suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (expected: Exception) {
    Result.failure(expected)
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
