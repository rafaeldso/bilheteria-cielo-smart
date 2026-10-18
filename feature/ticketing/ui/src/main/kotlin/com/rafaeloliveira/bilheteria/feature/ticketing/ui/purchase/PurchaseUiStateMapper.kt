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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.QuantityLimits

/** The event of the route in the catalog: not read yet, impossible to offer (BR-27), or found with its stock. */
internal sealed interface EventLookup {
    data object Unknown : EventLookup

    /** Catalog failure, unknown id, missing id or sold out. */
    data object Missing : EventLookup

    data class Found(
        val event: AvailableEvent,
    ) : EventLookup
}

/** Where the lookup of an open purchase left from an earlier run stands (BR-15, BR-26). */
internal enum class Recovery {
    /** Nothing failed (the lookup is not needed, was requested, or is under way). */
    Idle,

    /** The lookup could not be completed: only "Consultar de novo" (the lookup again) leaves this state. */
    Failed,
}

/** What the ViewModel knows about the open purchase (`Pending` or `Recovering`, BR-10). */
internal sealed interface OpenPurchase {
    /** Nothing was read yet: the screen must not offer "Pay" (BR-10). */
    data object Unknown : OpenPurchase

    data object None : OpenPurchase

    /** The open purchase could not be read; a new payment is not safe, so the screen shows `RecoveryFailed` (BR-26). */
    data object Unreadable : OpenPurchase

    data class Present(
        val purchase: Purchase,
    ) : OpenPurchase
}

/** How the last payment attempt ended, when it is not (or no longer) the open purchase. */
internal sealed interface AttemptOutcome {
    data object None : AttemptOutcome

    /** The attempt reached a final status: a sale goes to the receipt, `Failed` allows a new attempt (BR-14, BR-15). */
    data class Finished(
        val purchase: Purchase,
    ) : AttemptOutcome

    /** The attempt failed before any purchase record could settle it (not stored, app missing). */
    data class Failed(
        val reason: PurchaseErrorReason,
    ) : AttemptOutcome
}

/** Everything the screen state is derived from. */
internal data class PurchaseInputs(
    val open: OpenPurchase,
    val outcome: AttemptOutcome,
    val event: EventLookup,
    val quantity: Int,
    val recovery: Recovery,
)

/** Pure mapping from the ViewModel inputs to the [PurchaseUiState] (plan §4, spec section 6). */
internal fun PurchaseInputs.toUiState(): PurchaseUiState =
    when {
        open is OpenPurchase.Present -> {
            open.purchase.toOpenState(recovery)
        }

        open is OpenPurchase.Unknown -> {
            PurchaseUiState.Loading
        }

        open is OpenPurchase.Unreadable -> {
            withEvent { eventState(it, PurchaseErrorReason.RecoveryFailed) }
        }

        outcome is AttemptOutcome.Finished && outcome.purchase.status.isSale -> {
            PurchaseUiState.Completed(outcome.purchase.id)
        }

        outcome is AttemptOutcome.Finished -> {
            withEvent { outcome.purchase.toFailedState() }
        }

        else -> {
            withEvent { eventState(it, (outcome as? AttemptOutcome.Failed)?.reason) }
        }
    }

private fun PurchaseInputs.withEvent(state: (AvailableEvent) -> PurchaseUiState): PurchaseUiState =
    when (event) {
        EventLookup.Unknown -> PurchaseUiState.Loading
        EventLookup.Missing -> PurchaseUiState.Unavailable
        is EventLookup.Found -> state(event.event)
    }

/** Quantity actually offered: the saved value kept inside BR-07 for the current stock. */
internal fun clampQuantity(
    quantity: Int,
    available: Int,
): Int = quantity.coerceIn(QuantityLimits.MIN, maxOf(QuantityLimits.MIN, QuantityLimits.max(available)))

private fun PurchaseInputs.eventState(
    available: AvailableEvent,
    failure: PurchaseErrorReason?,
): PurchaseUiState {
    val selected = clampQuantity(quantity, available.available)
    val total = available.event.unitPrice * selected
    return if (failure != null) {
        PurchaseUiState.Error(available.toSummary(), selected, total, failure)
    } else {
        editing(available, selected, total)
    }
}

private fun editing(
    available: AvailableEvent,
    quantity: Int,
    total: Money,
): PurchaseUiState.Editing {
    val canIncrement = QuantityLimits.canIncrement(quantity, available.available)
    return PurchaseUiState.Editing(
        event = available.toSummary(),
        quantity = quantity,
        canDecrement = QuantityLimits.canDecrement(quantity),
        canIncrement = canIncrement,
        limitHint = if (canIncrement) null else limitHint(available.available),
        total = total,
    )
}

private fun limitHint(available: Int): LimitHint =
    if (available < QuantityLimits.MAX_PER_PURCHASE) {
        LimitHint.OnlyAvailable(available)
    } else {
        LimitHint.MaxPerPurchase(QuantityLimits.MAX_PER_PURCHASE)
    }

/** The snapshot of the open purchase decides the state; a lookup that failed shows the error instead (BR-26). */
private fun Purchase.toOpenState(recovery: Recovery = Recovery.Idle): PurchaseUiState =
    when {
        recovery == Recovery.Failed -> {
            PurchaseUiState.Error(toSummary(), quantity, total, PurchaseErrorReason.RecoveryFailed)
        }

        status == PurchaseStatus.Recovering -> {
            PurchaseUiState.Recovering(toSummary(), quantity, total)
        }

        else -> {
            PurchaseUiState.AwaitingPayment(toSummary(), quantity, total)
        }
    }

/** A finished attempt that is not a sale is `Failed`: the settle only runs once no purchase is open (BR-15). */
private fun Purchase.toFailedState(): PurchaseUiState {
    check(status == PurchaseStatus.Failed) { "unreachable: a settled attempt is a sale or Failed, not $status" }
    return PurchaseUiState.Error(toSummary(), quantity, total, failureReason.toErrorReason())
}

private fun FailureReason?.toErrorReason(): PurchaseErrorReason =
    when (this) {
        FailureReason.TerminalAppUnavailable -> PurchaseErrorReason.NoCieloApp
        FailureReason.Authentication -> PurchaseErrorReason.Authentication
        FailureReason.NotFound -> PurchaseErrorReason.RecoveryNotFound
        FailureReason.Generic, null -> PurchaseErrorReason.Generic
    }

private fun AvailableEvent.toSummary() =
    EventSummary(
        id = event.id,
        name = event.name,
        startsAt = event.startsAt,
        venue = event.venue,
        unitPrice = event.unitPrice,
        available = available,
    )

private fun Purchase.toSummary() =
    EventSummary(
        id = eventId,
        name = eventName,
        startsAt = eventStartsAt,
        venue = venue,
        unitPrice = unitPrice,
        available = null,
    )
