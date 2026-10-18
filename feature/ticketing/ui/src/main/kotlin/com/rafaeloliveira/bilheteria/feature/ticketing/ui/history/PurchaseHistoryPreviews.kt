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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.history

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.ZoneId

// Fictional data only (ui-design.md). Fixed "now" and zone keep the relative dates stable.
private val PreviewZone: ZoneId = ZoneId.of("America/Sao_Paulo")
private val PreviewNow: Instant = Instant.parse("2026-10-17T20:00:00Z")

private val PreviewContent =
    PurchaseHistoryUiState.Content(
        persistentListOf(
            HistoryItem(
                id = "p3",
                eventName = "Noite de Jazz no Parque",
                createdAt = Instant.parse("2026-10-17T17:32:00Z"),
                quantity = 2,
                total = Money(10_000),
                status = PurchaseStatus.Approved,
            ),
            HistoryItem(
                id = "p2",
                eventName = "Stand-up Comedy no Teatro Municipal",
                createdAt = Instant.parse("2026-10-16T21:10:00Z"),
                quantity = 1,
                total = Money(6_000),
                status = PurchaseStatus.Cancelled,
            ),
            HistoryItem(
                id = "p1",
                eventName = "Festival de Música Eletrônica e Artes Visuais do Litoral Norte",
                createdAt = Instant.parse("2026-10-15T12:00:00Z"),
                quantity = 1,
                total = Money(125_050),
                status = PurchaseStatus.Declined,
            ),
        ),
    )

@Composable
private fun HistoryPreview(uiState: PurchaseHistoryUiState) {
    BilheteriaTheme {
        PurchaseHistoryScreen(
            uiState = uiState,
            now = PreviewNow,
            zone = PreviewZone,
            onEvent = {},
            onNavigateUp = {},
        )
    }
}

@Preview(name = "Light", showSystemUi = true)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun PurchaseHistoryLoadingPreview() {
    HistoryPreview(PurchaseHistoryUiState.Loading)
}

@Preview(name = "Light", showSystemUi = true)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun PurchaseHistoryContentPreview() {
    HistoryPreview(PreviewContent)
}

@Preview(name = "Font 2x", fontScale = 2f, showSystemUi = true)
@Composable
private fun PurchaseHistoryContentLargeFontPreview() {
    HistoryPreview(PreviewContent)
}

@Preview(name = "Light", showSystemUi = true)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun PurchaseHistoryEmptyPreview() {
    HistoryPreview(PurchaseHistoryUiState.Empty)
}

@Preview(name = "Light", showSystemUi = true)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
private fun PurchaseHistoryErrorPreview() {
    HistoryPreview(PurchaseHistoryUiState.Error)
}

@Preview(name = "Font 2x", fontScale = 2f, showSystemUi = true)
@Composable
private fun PurchaseHistoryErrorLargeFontPreview() {
    HistoryPreview(PurchaseHistoryUiState.Error)
}
