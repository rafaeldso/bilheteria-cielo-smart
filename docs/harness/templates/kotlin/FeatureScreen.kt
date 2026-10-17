// Module: feature:balance:ui — Route (stateful) + Screen (stateless) + one @Preview per state.
package com.example.app.feature.balance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.core.designsystem.AppTheme
import com.example.app.core.designsystem.Spacing
import com.example.app.core.ui.formatCurrency
import com.example.app.feature.balance.domain.Balance
import com.example.app.feature.balance.domain.Money
import java.math.BigDecimal
import java.time.Instant

/** Only the Route knows the ViewModel, side effects and navigation. */
@Composable
fun BalanceRoute(
    modifier: Modifier = Modifier,
    viewModel: BalanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BalanceScreen(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

/** Stateless: renders every spec state explicitly (exhaustive when, no else). */
@Composable
fun BalanceScreen(
    uiState: BalanceUiState,
    onEvent: (BalanceUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(Spacing.large), contentAlignment = Alignment.Center) {
        when (uiState) {
            BalanceUiState.Loading -> CircularProgressIndicator()
            is BalanceUiState.Content -> BalanceContent(uiState.balance)
            is BalanceUiState.Error -> BalanceError(uiState.reason, onRetry = { onEvent(BalanceUiEvent.Retry) })
        }
    }
}

@Composable
private fun BalanceContent(balance: Balance, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(
            text = stringResource(R.string.balance_available_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = balance.available.formatCurrency(), style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
private fun BalanceError(reason: ErrorReason, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (reason) {
        ErrorReason.OFFLINE -> R.string.balance_error_offline
        ErrorReason.ACCOUNT_BLOCKED -> R.string.balance_error_blocked
        ErrorReason.GENERIC -> R.string.balance_error_generic
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(message))
        if (reason != ErrorReason.ACCOUNT_BLOCKED) {
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

// --- Previews: one per state (feed Roborazzi screenshot tests) --------------

private val previewBalance = Balance(
    available = Money(BigDecimal("1530.75")),
    blocked = Money(BigDecimal("120.00")),
    updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
)

@Preview(showBackground = true)
@Composable
private fun BalanceLoadingPreview() = AppTheme { BalanceScreen(BalanceUiState.Loading, onEvent = {}) }

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f, name = "Fonte 2x")
@Composable
private fun BalanceContentPreview() = AppTheme { BalanceScreen(BalanceUiState.Content(previewBalance), onEvent = {}) }

@Preview(showBackground = true)
@Composable
private fun BalanceErrorPreview() = AppTheme {
    BalanceScreen(BalanceUiState.Error(ErrorReason.OFFLINE), onEvent = {})
}
