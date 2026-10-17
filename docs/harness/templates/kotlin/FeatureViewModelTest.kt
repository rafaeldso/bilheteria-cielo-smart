// Module: feature:balance:ui (src/test). Every AC-xx from the spec appears in a test name.
package com.example.app.feature.balance.ui

import app.cash.turbine.test
import com.example.app.core.common.AppError
import com.example.app.core.common.DataResult
import com.example.app.core.testing.MainDispatcherExtension
import com.example.app.core.testing.balance.FakeBalanceRepository
import com.example.app.feature.balance.domain.Balance
import com.example.app.feature.balance.domain.BalanceError
import com.example.app.feature.balance.domain.GetBalanceUseCase
import com.example.app.feature.balance.domain.Money
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.math.BigDecimal
import java.time.Instant

class BalanceViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension() // replaces the Main dispatcher with a StandardTestDispatcher

    private val repository = FakeBalanceRepository()
    private fun createViewModel() = BalanceViewModel(GetBalanceUseCase(repository))

    @Test
    fun `AC-01 - dado saldo disponivel quando abrir a tela entao exibe o saldo`() = runTest {
        repository.enqueue(DataResult.Success(balance))

        createViewModel().uiState.test {
            assertEquals(BalanceUiState.Loading, awaitItem())
            assertEquals(BalanceUiState.Content(balance), awaitItem())
        }
    }

    @Test
    fun `AC-02 - dado sem conexao quando abrir a tela entao exibe erro offline`() = runTest {
        repository.enqueue(DataResult.Failure(AppError.Network.Offline))

        createViewModel().uiState.test {
            skipItems(1) // Loading
            assertEquals(BalanceUiState.Error(ErrorReason.OFFLINE), awaitItem())
        }
    }

    @Test
    fun `AC-03 - dado conta bloqueada quando abrir a tela entao exibe erro de bloqueio`() = runTest {
        repository.enqueue(DataResult.Failure(BalanceError.AccountBlocked))

        createViewModel().uiState.test {
            skipItems(1)
            assertEquals(BalanceUiState.Error(ErrorReason.ACCOUNT_BLOCKED), awaitItem())
        }
    }

    @Test
    fun `AC-04 - dado erro offline quando tocar em tentar novamente entao recarrega o saldo`() = runTest {
        repository.enqueue(DataResult.Failure(AppError.Network.Offline), DataResult.Success(balance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(2) // Loading, Error
            viewModel.onEvent(BalanceUiEvent.Retry)
            assertEquals(BalanceUiState.Loading, awaitItem())
            assertEquals(BalanceUiState.Content(balance), awaitItem())
        }
        assertEquals(2, repository.calls)
    }

    private companion object {
        val balance = Balance(
            available = Money(BigDecimal("100.00")),
            blocked = Money(BigDecimal.ZERO),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
    }
}
