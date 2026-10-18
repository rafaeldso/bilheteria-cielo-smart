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

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R

/**
 * Whether the system back is ignored: an open payment or a lookup of one must not be abandoned (BR-10, BR-15,
 * BR-25; ui-spec SCR-02, section 4.5).
 */
internal fun PurchaseUiState.blocksSystemBack(): Boolean =
    when (this) {
        is PurchaseUiState.AwaitingPayment, is PurchaseUiState.Recovering -> true

        PurchaseUiState.Loading,
        is PurchaseUiState.Editing,
        is PurchaseUiState.Error,
        PurchaseUiState.Unavailable,
        is PurchaseUiState.Completed,
        -> false
    }

/**
 * Whether the top app bar offers "Voltar para eventos": not while a payment is open or being confirmed, and not while
 * the screen is already handing over to the receipt (ui-spec SCR-02, section 4.1).
 */
internal fun PurchaseUiState.showsNavigateUp(): Boolean =
    when (this) {
        PurchaseUiState.Loading,
        is PurchaseUiState.Editing,
        is PurchaseUiState.Error,
        PurchaseUiState.Unavailable,
        -> true

        is PurchaseUiState.AwaitingPayment,
        is PurchaseUiState.Recovering,
        is PurchaseUiState.Completed,
        -> false
    }

/** What the retry button of an `Error` does, and so what it says (ui-spec SCR-02, section 4.4). */
internal enum class RetryAction {
    /** Opens a new payment with the same quantity: "Tentar pagar R$ X de novo". */
    PayAgain,

    /** Repeats the Cielo lookup and never charges (BR-26): "Consultar de novo". */
    CheckAgain,
}

internal fun PurchaseErrorReason.retryAction(): RetryAction =
    when (this) {
        PurchaseErrorReason.RecoveryFailed -> RetryAction.CheckAgain

        PurchaseErrorReason.NoCieloApp,
        PurchaseErrorReason.Generic,
        PurchaseErrorReason.Authentication,
        PurchaseErrorReason.RecoveryNotFound,
        -> RetryAction.PayAgain
    }

@StringRes
internal fun PurchaseErrorReason.titleRes(): Int =
    when (this) {
        PurchaseErrorReason.NoCieloApp -> R.string.ticketing_purchase_error_no_app_title
        PurchaseErrorReason.Generic -> R.string.ticketing_purchase_error_generic_title
        PurchaseErrorReason.Authentication -> R.string.ticketing_purchase_error_auth_title
        PurchaseErrorReason.RecoveryNotFound -> R.string.ticketing_purchase_error_not_found_title
        PurchaseErrorReason.RecoveryFailed -> R.string.ticketing_purchase_error_recovery_title
    }

@StringRes
internal fun PurchaseErrorReason.bodyRes(): Int =
    when (this) {
        PurchaseErrorReason.NoCieloApp -> R.string.ticketing_purchase_error_no_app_body
        PurchaseErrorReason.Generic -> R.string.ticketing_purchase_error_generic_body
        PurchaseErrorReason.Authentication -> R.string.ticketing_purchase_error_auth_body
        PurchaseErrorReason.RecoveryNotFound -> R.string.ticketing_purchase_error_not_found_body
        PurchaseErrorReason.RecoveryFailed -> R.string.ticketing_purchase_error_recovery_body
    }

/** The hint under the stepper: a resource and the number it carries (ui-spec SCR-02 strings `limit_*`). */
internal sealed interface LimitHintText {
    data class Fixed(
        @StringRes val resId: Int,
        val value: Int,
    ) : LimitHintText

    data class Counted(
        @PluralsRes val resId: Int,
        val count: Int,
    ) : LimitHintText
}

internal fun LimitHint.text(): LimitHintText =
    when (this) {
        is LimitHint.MaxPerPurchase -> LimitHintText.Fixed(R.string.ticketing_purchase_limit_max, max)
        is LimitHint.OnlyAvailable -> LimitHintText.Counted(R.plurals.ticketing_purchase_limit_available, available)
    }
