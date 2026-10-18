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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Clock
import java.time.Year

/**
 * Stateful entry of SCR-01: connects [EventListViewModel] to [EventListScreen].
 *
 * Opening an event and opening the history are navigation, not state, so they do not reach the ViewModel: the
 * route turns them into [onEventSelected] and [onOpenHistory]. Every other event goes to
 * [EventListViewModel.onEvent].
 *
 * @param onEventSelected called with the event id to open SCR-02.
 * @param onOpenHistory called to open SCR-04.
 * @param clock source of the zone and of the current year; the default is the system clock and zone. The screen
 * itself never reads the clock: the route resolves it once and hands the zone and the year down.
 */
@Composable
internal fun EventListRoute(
    onEventSelected: (id: String) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventListViewModel = hiltViewModel(),
    clock: Clock = Clock.systemDefaultZone(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnEventSelected by rememberUpdatedState(onEventSelected)
    val currentOnOpenHistory by rememberUpdatedState(onOpenHistory)
    val formats = remember(clock) { EventListFormats(zoneId = clock.zone, currentYear = Year.now(clock).value) }
    val onEvent =
        remember(viewModel) {
            { event: EventListUiEvent ->
                when (event) {
                    is EventListUiEvent.OnEventSelected -> currentOnEventSelected(event.id)
                    EventListUiEvent.OnOpenHistory -> currentOnOpenHistory()
                    EventListUiEvent.OnRetry, is EventListUiEvent.OnMonthSelected -> viewModel.onEvent(event)
                }
            }
        }
    EventListScreen(uiState = uiState, formats = formats, onEvent = onEvent, modifier = modifier)
}
