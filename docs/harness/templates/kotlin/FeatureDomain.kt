// Module: feature:balance:domain (Kotlin JVM — no android.*, no data/ui types)
// Packages: ...domain.model, ...domain.repository, ...domain.usecase
package com.example.app.feature.balance.domain

import com.example.app.core.common.AppError
import com.example.app.core.common.DataResult
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

// --- model ------------------------------------------------------------------

/** Money is never Double/Float (AP-08). */
@JvmInline
value class Money(val amount: BigDecimal)

data class Balance(
    val available: Money,
    val blocked: Money,
    val updatedAt: Instant,
)

/** Feature errors are named by business rule, not by HTTP code. */
sealed interface BalanceError : AppError {
    data object AccountBlocked : BalanceError
}

// --- repository contract (implemented in :data) -----------------------------

interface BalanceRepository {
    suspend fun getBalance(): DataResult<Balance>
}

// --- use case: one public operator invoke, one business action ---------------

/** BR-01: the system shall show the available balance including blocked amounts. */
class GetBalanceUseCase @Inject constructor(
    private val repository: BalanceRepository,
) {
    suspend operator fun invoke(): DataResult<Balance> = repository.getBalance()
}
