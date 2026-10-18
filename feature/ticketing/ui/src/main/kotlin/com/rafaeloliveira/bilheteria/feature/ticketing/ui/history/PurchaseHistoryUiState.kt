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

import androidx.compose.runtime.Immutable
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import kotlinx.collections.immutable.ImmutableList
import java.time.Instant

/** States of SCR-04 (spec section 6). */
@Immutable
internal sealed interface PurchaseHistoryUiState {
    /** Reading in progress (AC-18). */
    data object Loading : PurchaseHistoryUiState

    /** Sales from the newest to the oldest (BR-18, AC-18). */
    data class Content(
        val items: ImmutableList<HistoryItem>,
    ) : PurchaseHistoryUiState

    /** No sale recorded (BR-20, AC-20). */
    data object Empty : PurchaseHistoryUiState

    /** The history could not be read; the operator can retry (BR-21, AC-21). */
    data object Error : PurchaseHistoryUiState
}

/**
 * One row of the history. Carries raw values: relative date, money and status text are
 * formatted by the screen (T23), never here. [status] is always a sale (approved, declined or cancelled).
 */
@Immutable
internal data class HistoryItem(
    val id: String,
    val eventName: String,
    val createdAt: Instant,
    val quantity: Int,
    val total: Money,
    val status: PurchaseStatus,
)
