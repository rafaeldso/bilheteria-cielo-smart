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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.tickets

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.ShareBundle
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.ZoneId

// One Light/Dark pair per state of SCR-05 (compose-ui.md, section 11) and per print failure, plus the tickets at 200%
// font. The QR Codes are fictitious patterns (not valid codes) and the event is invented: no real purchase.

private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 760
private const val PREVIEW_FONT_SCALE = 2f
private const val PREVIEW_QR_SIDE = 25
private const val PREVIEW_FINDER = 7
private const val PREVIEW_TICKETS = 3
private const val FINDER_PERIOD = 6
private const val X_MIX = 7
private const val Y_MIX = 13
private const val MIX_MODULUS = 3
private val PreviewZone: ZoneId = ZoneId.of("America/Sao_Paulo")

private fun previewMatrix(): QrMatrix {
    val side = PREVIEW_QR_SIDE
    val modules =
        BooleanArray(side * side) { index ->
            val x = index % side
            val y = index / side
            val finder =
                (x < PREVIEW_FINDER && y < PREVIEW_FINDER) ||
                    (x >= side - PREVIEW_FINDER && y < PREVIEW_FINDER) ||
                    (x < PREVIEW_FINDER && y >= side - PREVIEW_FINDER)
            if (finder) x % FINDER_PERIOD != 1 && y % FINDER_PERIOD != 1 else (x * X_MIX + y * Y_MIX) % MIX_MODULUS == 0
        }
    return QrMatrix(side, modules)
}

private fun previewContent(action: TicketActionState) =
    TicketsUiState.Content(
        event =
            TicketEvent(
                "Noite de Jazz no Parque",
                Instant.parse("2026-11-15T23:00:00Z"),
                "Concha Acústica Municipal",
            ),
        tickets = (1..PREVIEW_TICKETS).map { TicketItem(it, PREVIEW_TICKETS, previewMatrix()) }.toImmutableList(),
        action = action,
    )

@Composable
private fun TicketsPreview(uiState: TicketsUiState) {
    BilheteriaTheme {
        TicketsScreen(uiState = uiState, zoneId = PreviewZone, onEvent = {}, onNavigateUp = {})
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
private fun TicketsLoadingPreview() = TicketsPreview(TicketsUiState.Loading)

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun TicketsIdlePreview() = TicketsPreview(previewContent(TicketActionState.Idle))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun TicketsPrintingPreview() = TicketsPreview(previewContent(TicketActionState.Printing(1, PREVIEW_TICKETS)))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun TicketsPrintFailedNoPaperPreview() =
    TicketsPreview(previewContent(TicketActionState.PrintFailed(PrintFailureReason.NoPaper, 1, PREVIEW_TICKETS)))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun TicketsPrintFailedTerminalPreview() =
    TicketsPreview(
        previewContent(TicketActionState.PrintFailed(PrintFailureReason.TerminalAppUnavailable, 0, PREVIEW_TICKETS)),
    )

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun TicketsPrintFailedPrinterPreview() =
    TicketsPreview(previewContent(TicketActionState.PrintFailed(PrintFailureReason.PrinterError, 2, PREVIEW_TICKETS)))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun TicketsPrintFailedTimeoutPreview() =
    TicketsPreview(previewContent(TicketActionState.PrintFailed(PrintFailureReason.Timeout, 1, PREVIEW_TICKETS)))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun TicketsPrintFailedUnknownPreview() =
    TicketsPreview(previewContent(TicketActionState.PrintFailed(PrintFailureReason.Unknown, 0, PREVIEW_TICKETS)))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun TicketsPrintedPreview() = TicketsPreview(previewContent(TicketActionState.Printed))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun TicketsShareFailedPreview() = TicketsPreview(previewContent(TicketActionState.ShareFailed))

@Preview(name = "Light", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Dark",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun TicketsErrorPreview() = TicketsPreview(TicketsUiState.Error)

@Preview(
    name = "Font 2x",
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    fontScale = PREVIEW_FONT_SCALE,
)
@Composable
private fun TicketsIdleLargeFontPreview() = TicketsPreview(previewContent(TicketActionState.Idle))

/** Keeps the share signal state visible to the previews' reader: it renders like [TicketActionState.Idle]. */
@Preview(name = "ShareReady", showBackground = true, widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun TicketsShareReadyPreview() =
    TicketsPreview(previewContent(TicketActionState.ShareReady(ShareBundle(emptyList()))))

@Preview(
    name = "Font 2x · PrintFailed",
    showBackground = true,
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    fontScale = PREVIEW_FONT_SCALE,
)
@Composable
private fun TicketsPrintFailedLargeFontPreview() =
    TicketsPreview(previewContent(TicketActionState.PrintFailed(PrintFailureReason.NoPaper, 1, PREVIEW_TICKETS)))
