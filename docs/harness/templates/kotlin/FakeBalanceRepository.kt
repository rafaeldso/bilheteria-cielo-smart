// Module: core:testing (or feature:balance:domain testFixtures)
// Fakes implement the real interface (LSP) and are preferred over mocks for repositories.
package com.example.app.core.testing.balance

import com.example.app.core.common.DataResult
import com.example.app.feature.balance.domain.Balance
import com.example.app.feature.balance.domain.BalanceRepository

class FakeBalanceRepository : BalanceRepository {
    private val results = ArrayDeque<DataResult<Balance>>()
    var calls = 0
        private set

    /** Queues results returned in order; the last one repeats. */
    fun enqueue(vararg result: DataResult<Balance>) {
        results.addAll(result)
    }

    override suspend fun getBalance(): DataResult<Balance> {
        calls++
        check(results.isNotEmpty()) { "FakeBalanceRepository: enqueue a result before calling getBalance()" }
        return if (results.size > 1) results.removeFirst() else results.first()
    }
}
