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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.eventlist

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.Instant
import java.time.ZoneOffset

// Fictional data, the same as the board (design/mockups.html): events, venues and amounts are invented.
private val PreviewFormats = EventListFormats(zoneId = ZoneOffset.UTC, currentYear = 2026)

private val November = EventMonth(2026, 11)
private val December = EventMonth(2026, 12)
private val January = EventMonth(2027, 1)

private fun previewEvent(
    name: String,
    startsAt: String,
    venue: String,
    priceInCents: Long,
    available: Int,
) = AvailableEvent(
    event =
        Event(
            id = startsAt,
            name = name,
            startsAt = Instant.parse(startsAt),
            venue = venue,
            unitPrice = Money(priceInCents),
            totalTickets = available,
        ),
    available = available,
)

private val NovemberEvents =
    listOf(
        previewEvent("Noite de Jazz no Parque", "2026-11-15T20:00:00Z", "Concha Acústica Municipal", 5_000, 48),
        previewEvent("Stand-up: Rindo à Toa", "2026-11-21T21:30:00Z", "Teatro do Centro", 6_000, 120),
        previewEvent("Orquestra Jovem: Clássicos do Cinema", "2026-11-30T17:00:00Z", "Auditório da Cidade", 3_500, 3),
    )
private val DecemberEvents =
    listOf(
        previewEvent("Coral de Fim de Ano na Praça", "2026-12-12T19:00:00Z", "Praça Central", 2_000, 200),
        previewEvent("Festival Indie Verão", "2026-12-19T18:00:00Z", "Arena Central", 8_000, 300),
    )
private val JanuaryEvents =
    listOf(
        previewEvent("Circo Lona Azul", "2027-01-10T16:00:00Z", "Parque de Exposições", 4_000, 150),
        previewEvent("Peça: A Casa das Sete Portas", "2027-01-24T20:00:00Z", "Teatro do Centro", 5_500, 80),
    )

private val AllMonths = persistentListOf(MonthOption(November), MonthOption(December), MonthOption(January))

private fun group(
    month: EventMonth,
    events: List<AvailableEvent>,
) = MonthGroup(month, events.toImmutableList())

private val AllMonthsContent =
    EventListUiState.Content(
        months = AllMonths,
        selectedMonth = null,
        groups =
            persistentListOf(
                group(November, NovemberEvents),
                group(December, DecemberEvents),
                group(January, JanuaryEvents),
            ),
    )

private val DecemberContent =
    EventListUiState.Content(
        months = AllMonths,
        selectedMonth = December,
        groups = persistentListOf(group(December, DecemberEvents)),
    )

@Composable
private fun EventListPreviewFrame(uiState: EventListUiState) {
    BilheteriaTheme { EventListScreen(uiState = uiState, formats = PreviewFormats, onEvent = {}) }
}

@Preview(name = "Light", widthDp = 360, heightDp = 720)
@Preview(name = "Dark", widthDp = 360, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EventListLoadingPreview() {
    EventListPreviewFrame(EventListUiState.Loading)
}

@Preview(name = "Light", widthDp = 360, heightDp = 720)
@Preview(name = "Dark", widthDp = 360, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EventListContentAllPreview() {
    EventListPreviewFrame(AllMonthsContent)
}

@Preview(name = "Light", widthDp = 360, heightDp = 720)
@Preview(name = "Dark", widthDp = 360, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EventListContentMonthFilteredPreview() {
    EventListPreviewFrame(DecemberContent)
}

@Preview(name = "Font 2x", widthDp = 360, heightDp = 720, fontScale = 2f)
@Composable
private fun EventListContentLargeFontPreview() {
    EventListPreviewFrame(AllMonthsContent)
}

@Preview(name = "Light", widthDp = 360, heightDp = 720)
@Preview(name = "Dark", widthDp = 360, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EventListEmptyPreview() {
    EventListPreviewFrame(EventListUiState.Empty)
}

@Preview(name = "Light", widthDp = 360, heightDp = 720)
@Preview(name = "Dark", widthDp = 360, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EventListErrorPreview() {
    EventListPreviewFrame(EventListUiState.Error)
}
