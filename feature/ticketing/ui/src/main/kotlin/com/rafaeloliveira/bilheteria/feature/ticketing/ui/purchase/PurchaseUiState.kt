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

import androidx.compose.runtime.Immutable
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.time.Instant

/**
 * State of SCR-02 (plan §4, spec section 6). `Loading` and `Completed` are additions approved on 2026-10-18:
 * `Loading` covers the moment before the event and the open purchase are known (the screen must not offer "Pay"
 * before that, BR-10); `Completed` is the navigation signal to the receipt (navigation is state).
 */
@Immutable
internal sealed interface PurchaseUiState {
    data object Loading : PurchaseUiState

    data class Editing(
        val event: EventSummary,
        val quantity: Int,
        val canDecrement: Boolean,
        val canIncrement: Boolean,
        val limitHint: LimitHint?,
        val total: Money,
    ) : PurchaseUiState

    data class AwaitingPayment(
        val event: EventSummary,
        val quantity: Int,
        val total: Money,
    ) : PurchaseUiState

    data class Recovering(
        val event: EventSummary,
        val quantity: Int,
        val total: Money,
    ) : PurchaseUiState

    data class Error(
        val event: EventSummary,
        val quantity: Int,
        val total: Money,
        val reason: PurchaseErrorReason,
    ) : PurchaseUiState

    /** BR-27: the event cannot be loaded (or is gone) and no purchase is open; the Route offers the way back. */
    data object Unavailable : PurchaseUiState

    /** The purchase reached Approved, Declined or Cancelled: the Route navigates to the receipt of [purchaseId]. */
    data class Completed(
        val purchaseId: String,
    ) : PurchaseUiState
}

/**
 * Which message the `Error` state shows (ui-spec SCR-02). [RecoveryFailed] is a lookup that could not be completed
 * (BR-26): "Tentar novamente" repeats the lookup and never opens a payment.
 */
internal enum class PurchaseErrorReason { NoCieloApp, Generic, Authentication, RecoveryNotFound, RecoveryFailed }

/** Why the quantity cannot grow (ui-spec SCR-02 strings `limit_max` and `limit_available`). */
@Immutable
internal sealed interface LimitHint {
    data class MaxPerPurchase(
        val max: Int,
    ) : LimitHint

    data class OnlyAvailable(
        val available: Int,
    ) : LimitHint
}

/**
 * Event header of the purchase screen. [available] is null when the summary comes from the purchase snapshot
 * (`AwaitingPayment`, `Recovering` and failures), where the current stock is not part of the record.
 */
@Immutable
internal data class EventSummary(
    val id: String,
    val name: String,
    val startsAt: Instant,
    val venue: String,
    val unitPrice: Money,
    val available: Int?,
)
