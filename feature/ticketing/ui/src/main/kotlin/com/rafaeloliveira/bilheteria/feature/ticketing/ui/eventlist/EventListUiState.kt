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

import androidx.compose.runtime.Immutable
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import kotlinx.collections.immutable.ImmutableList

/** A month chip of the filter; "Todos" is not an option, it is `selectedMonth == null` (BR-23). */
@Immutable
internal data class MonthOption(
    val month: EventMonth,
)

/** Events of one month under a single header (BR-22). */
@Immutable
internal data class MonthGroup(
    val month: EventMonth,
    val events: ImmutableList<AvailableEvent>,
)

/** State of SCR-01 (spec §6). */
@Immutable
internal sealed interface EventListUiState {
    data object Loading : EventListUiState

    /**
     * @property months months with at least one available event, in order (BR-23).
     * @property selectedMonth the active filter; null means "Todos" (BR-23, BR-24).
     * @property groups every month when [selectedMonth] is null, otherwise only the selected one (BR-22, BR-23).
     */
    data class Content(
        val months: ImmutableList<MonthOption>,
        val selectedMonth: EventMonth?,
        val groups: ImmutableList<MonthGroup>,
    ) : EventListUiState

    data object Empty : EventListUiState

    data object Error : EventListUiState
}
