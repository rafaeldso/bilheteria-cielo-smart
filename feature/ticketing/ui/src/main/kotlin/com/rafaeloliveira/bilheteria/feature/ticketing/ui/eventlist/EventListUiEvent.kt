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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth

/** Events of SCR-01 (spec §6). */
sealed interface EventListUiEvent {
    data object OnRetry : EventListUiEvent

    data class OnEventSelected(
        val id: String,
    ) : EventListUiEvent

    /** `null` selects "Todos" (BR-23). */
    data class OnMonthSelected(
        val month: EventMonth?,
    ) : EventListUiEvent

    data object OnOpenHistory : EventListUiEvent
}
