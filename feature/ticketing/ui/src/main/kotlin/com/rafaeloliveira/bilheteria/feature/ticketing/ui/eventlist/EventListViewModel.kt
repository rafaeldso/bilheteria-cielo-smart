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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.GroupEventsByMonthUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveAvailableEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * SCR-01 (AC-01, AC-02, AC-03, AC-17, AC-22, AC-23, AC-24).
 *
 * The list is derived from the live catalog and approved purchases, so a sale changes the availability
 * without a manual reload (BR-17). The month filter lives here and survives while the list is in the back
 * stack; a month without events resets it to "Todos" (BR-24).
 *
 * [EventListUiEvent.OnEventSelected] and [EventListUiEvent.OnOpenHistory] only navigate; the `Route`
 * handles them, so they do not change this state.
 */
@HiltViewModel
internal class EventListViewModel
    @Inject
    constructor(
        private val observeAvailableEvents: ObserveAvailableEventsUseCase,
        private val groupEventsByMonth: GroupEventsByMonthUseCase,
    ) : ViewModel() {
        private val selectedMonth = MutableStateFlow<EventMonth?>(null)
        private val attempt = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<EventListUiState> =
            attempt
                .flatMapLatest { load() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EventListUiState.Loading)

        fun onEvent(event: EventListUiEvent) {
            when (event) {
                EventListUiEvent.OnRetry -> attempt.update { it + 1 }
                is EventListUiEvent.OnMonthSelected -> selectedMonth.update { event.month }
                is EventListUiEvent.OnEventSelected, EventListUiEvent.OnOpenHistory -> Unit
            }
        }

        private fun load(): Flow<EventListUiState> =
            combine(observeAvailableEvents(), selectedMonth, ::toUiState)
                .onStart { emit(EventListUiState.Loading) }

        private fun toUiState(
            result: DataResult<List<AvailableEvent>>,
            requested: EventMonth?,
        ): EventListUiState =
            when (result) {
                is DataResult.Failure -> {
                    EventListUiState.Error
                }

                is DataResult.Success -> {
                    val grouped = groupEventsByMonth(result.value, requested)
                    // BR-24: a stale filter is dropped for good, so it does not come back if the month reappears.
                    selectedMonth.compareAndSet(requested, grouped.selected)
                    if (grouped.months.isEmpty()) {
                        EventListUiState.Empty
                    } else {
                        EventListUiState.Content(
                            months = grouped.months.map(::MonthOption).toImmutableList(),
                            selectedMonth = grouped.selected,
                            groups =
                                grouped.groups
                                    .map {
                                        MonthGroup(
                                            it.month,
                                            it.events.toImmutableList(),
                                        )
                                    }.toImmutableList(),
                        )
                    }
                }
            }
    }
