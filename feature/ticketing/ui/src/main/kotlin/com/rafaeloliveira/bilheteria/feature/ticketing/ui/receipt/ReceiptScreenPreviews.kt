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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.time.Instant
import java.time.ZoneId

// One Light/Dark pair per ReceiptUiState (compose-ui.md, section 11), plus the approved receipt at 200% font.
// The data is fictional: no real card, NSU or buyer.

private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 900
private const val PREVIEW_UNIT_CENTS = 5_000L
private const val PREVIEW_TOTAL_CENTS = 10_000L
private const val PREVIEW_FONT_SCALE = 2f
private val PreviewZone: ZoneId = ZoneId.of("America/Sao_Paulo")

private val PreviewReceipt =
    ReceiptData(
        purchaseNumber = "7F3K-9Q2L",
        eventName = "Noite de Jazz no Parque",
        eventStartsAt = Instant.parse("2026-11-15T23:00:00Z"),
        venue = "Concha Acústica Municipal",
        quantity = 2,
        unitPrice = Money(PREVIEW_UNIT_CENTS),
        total = Money(PREVIEW_TOTAL_CENTS),
        purchasedAt = Instant.parse("2026-10-17T17:32:00Z"),
        transaction = null,
    )

private val PreviewApproved =
    PreviewReceipt.copy(
        transaction = ReceiptTransaction(brand = "Visa", authCode = "A12345", nsu = "000123"),
    )

@Composable
private fun ReceiptPreview(
    uiState: ReceiptUiState,
    origin: ReceiptOrigin,
) {
    BilheteriaTheme {
        ReceiptScreen(
            uiState = uiState,
            origin = origin,
            onEvent = {},
            onNavigateUp = {},
            zoneId = PreviewZone,
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenLoadingPreview() {
    ReceiptPreview(uiState = ReceiptUiState.Loading, origin = ReceiptOrigin.Sale)
}

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenApprovedPreview() {
    ReceiptPreview(uiState = ReceiptUiState.Approved(PreviewApproved), origin = ReceiptOrigin.Sale)
}

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenDeclinedPreview() {
    ReceiptPreview(uiState = ReceiptUiState.Declined(PreviewReceipt), origin = ReceiptOrigin.Sale)
}

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenCancelledFromHistoryPreview() {
    ReceiptPreview(uiState = ReceiptUiState.Cancelled(PreviewReceipt), origin = ReceiptOrigin.History)
}

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenErrorPreview() {
    ReceiptPreview(uiState = ReceiptUiState.Error, origin = ReceiptOrigin.History)
}

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenErrorFromSalePreview() {
    ReceiptPreview(uiState = ReceiptUiState.Error, origin = ReceiptOrigin.Sale)
}

@Preview(
    name = "Font 2x",
    fontScale = PREVIEW_FONT_SCALE,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun ReceiptScreenApprovedLargeFontPreview() {
    ReceiptPreview(uiState = ReceiptUiState.Approved(PreviewApproved), origin = ReceiptOrigin.Sale)
}
