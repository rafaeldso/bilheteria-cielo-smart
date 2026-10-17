// Module: feature:balance:ui — UiState/UiEvent mirror section 6 of the spec exactly.
package com.example.app.feature.balance.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.core.common.AppError
import com.example.app.core.common.DataResult
import com.example.app.feature.balance.domain.Balance
import com.example.app.feature.balance.domain.BalanceError
import com.example.app.feature.balance.domain.GetBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
sealed interface BalanceUiState {
    data object Loading : BalanceUiState
    data class Content(val balance: Balance) : BalanceUiState
    data class Error(val reason: ErrorReason) : BalanceUiState
}

enum class ErrorReason { OFFLINE, ACCOUNT_BLOCKED, GENERIC }

sealed interface BalanceUiEvent {
    data object Retry : BalanceUiEvent
}

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val getBalance: GetBalanceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BalanceUiState>(BalanceUiState.Loading)
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: BalanceUiEvent) {
        when (event) {
            BalanceUiEvent.Retry -> load()
        }
    }

    private fun load() {
        _uiState.value = BalanceUiState.Loading
        viewModelScope.launch {
            val newState = when (val result = getBalance()) {
                is DataResult.Success -> BalanceUiState.Content(result.value)
                is DataResult.Failure -> BalanceUiState.Error(result.error.toReason())
            }
            _uiState.update { newState }
        }
    }

    private fun AppError.toReason(): ErrorReason = when (this) {
        is AppError.Network -> ErrorReason.OFFLINE
        BalanceError.AccountBlocked -> ErrorReason.ACCOUNT_BLOCKED
        else -> ErrorReason.GENERIC
    }
}
