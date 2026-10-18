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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.time.Instant
import java.time.ZoneId

// One preview pair (Light and Dark) per state of the UI Spec, with the fictitious data of the board
// (docs/specs/001-venda-de-ingressos/design/mockups.html).

private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 780
private const val UNIT_PRICE_CENTS = 5_000L
private const val LARGE_FONT_SCALE = 2f
private const val MAX_PER_PURCHASE = 10

// A fixed zone, so the previews show the same time whatever the machine that renders them (20:00 here).
private val previewZone: ZoneId = ZoneId.of("America/Sao_Paulo")

private val previewEvent =
    EventSummary(
        id = "e1",
        name = "Noite de Jazz no Parque",
        startsAt = Instant.parse("2026-11-15T23:00:00Z"),
        venue = "Concha Acústica Municipal",
        unitPrice = Money(UNIT_PRICE_CENTS),
        available = 48,
    )

/** The snapshot of a purchase has no stock (`available` null). */
private val previewSnapshot = previewEvent.copy(available = null)

private val previewEditing =
    PurchaseUiState.Editing(
        event = previewEvent,
        quantity = 2,
        canDecrement = true,
        canIncrement = true,
        limitHint = null,
        total = Money(UNIT_PRICE_CENTS * 2),
    )

private val previewLimit =
    PurchaseUiState.Editing(
        event = previewEvent.copy(id = "e3", name = "Orquestra Jovem: Clássicos do Cinema", available = 3),
        quantity = 3,
        canDecrement = true,
        canIncrement = false,
        limitHint = LimitHint.OnlyAvailable(available = 3),
        total = Money(UNIT_PRICE_CENTS * 3),
    )

private val previewMaxPerPurchase =
    PurchaseUiState.Editing(
        event = previewEvent,
        quantity = MAX_PER_PURCHASE,
        canDecrement = true,
        canIncrement = false,
        limitHint = LimitHint.MaxPerPurchase(max = MAX_PER_PURCHASE),
        total = Money(UNIT_PRICE_CENTS * MAX_PER_PURCHASE),
    )

private fun previewError(reason: PurchaseErrorReason) =
    PurchaseUiState.Error(
        event = previewSnapshot,
        quantity = 2,
        total = Money(UNIT_PRICE_CENTS * 2),
        reason = reason,
    )

@Composable
private fun PreviewPurchase(state: PurchaseUiState) {
    BilheteriaTheme {
        PurchaseScreen(uiState = state, zone = previewZone, onEvent = {}, onNavigateUp = {}, onBackToEvents = {})
    }
}

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseLoadingPreview() = PreviewPurchase(PurchaseUiState.Loading)

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseEditingPreview() = PreviewPurchase(previewEditing)

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseEditingLimitPreview() = PreviewPurchase(previewLimit)

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseEditingMaxPerPurchasePreview() = PreviewPurchase(previewMaxPerPurchase)

@Preview(name = "Font 2x", fontScale = LARGE_FONT_SCALE, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseEditingLargeFontPreview() = PreviewPurchase(previewEditing)

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseAwaitingPaymentPreview() =
    PreviewPurchase(
        PurchaseUiState.AwaitingPayment(
            event = previewSnapshot,
            quantity = 2,
            total = Money(UNIT_PRICE_CENTS * 2),
        ),
    )

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseRecoveringPreview() =
    PreviewPurchase(
        PurchaseUiState.Recovering(
            event = previewSnapshot,
            quantity = 2,
            total = Money(UNIT_PRICE_CENTS * 2),
        ),
    )

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseErrorNoCieloAppPreview() = PreviewPurchase(previewError(PurchaseErrorReason.NoCieloApp))

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseErrorGenericPreview() = PreviewPurchase(previewError(PurchaseErrorReason.Generic))

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseErrorAuthenticationPreview() = PreviewPurchase(previewError(PurchaseErrorReason.Authentication))

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseErrorRecoveryNotFoundPreview() = PreviewPurchase(previewError(PurchaseErrorReason.RecoveryNotFound))

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseErrorRecoveryFailedPreview() = PreviewPurchase(previewError(PurchaseErrorReason.RecoveryFailed))

@Preview(name = "Font 2x", fontScale = LARGE_FONT_SCALE, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseErrorLargeFontPreview() = PreviewPurchase(previewError(PurchaseErrorReason.Generic))

@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseUnavailablePreview() = PreviewPurchase(PurchaseUiState.Unavailable)

/** Same look as `Loading`: it only lasts until the Route has navigated to the receipt. */
@Preview(name = "Light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun PurchaseCompletedPreview() = PreviewPurchase(PurchaseUiState.Completed(purchaseId = "7f3k-9q2l"))
