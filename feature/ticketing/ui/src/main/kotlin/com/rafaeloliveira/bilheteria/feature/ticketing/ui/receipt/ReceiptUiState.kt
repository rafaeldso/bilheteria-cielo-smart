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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import androidx.compose.runtime.Immutable
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.time.Instant

/**
 * State of the receipt screen (SCR-03). [Loading] is only the internal load of the local record (no board, plan §4);
 * the three result states mirror the spec §6 and carry the same [ReceiptData]; [Error] is a record that cannot be
 * shown.
 */
@Immutable
internal sealed interface ReceiptUiState {
    data object Loading : ReceiptUiState

    /** BR-11, AC-09: approved purchase, with the transaction data (AC-16). */
    data class Approved(
        val receipt: ReceiptData,
    ) : ReceiptUiState

    /** BR-13, AC-11: declined purchase, without transaction data (AC-16). */
    data class Declined(
        val receipt: ReceiptData,
    ) : ReceiptUiState

    /** BR-12, AC-10: purchase cancelled at the terminal, without transaction data (AC-16). */
    data class Cancelled(
        val receipt: ReceiptData,
    ) : ReceiptUiState

    /** BR-28, AC-27: the record cannot be read, does not exist or has no sale result; the operator can retry. */
    data object Error : ReceiptUiState
}

/**
 * Receipt fields of BR-16. Money and instants stay typed: the screen formats them (pt-BR). There is no field for the
 * card number: Cielo never sends it to the app and it is never shown (BR-16).
 *
 * @property purchaseNumber the purchase reference, the "Nº da compra" of the receipt.
 * @property transaction present only when the purchase is approved (AC-16).
 */
@Immutable
internal data class ReceiptData(
    val purchaseNumber: String,
    val eventName: String,
    val eventStartsAt: Instant,
    val venue: String,
    val quantity: Int,
    val unitPrice: Money,
    val total: Money,
    val purchasedAt: Instant,
    val transaction: ReceiptTransaction?,
)

/** Transaction data of an approved purchase: brand, authorization code and NSU (BR-16). */
@Immutable
internal data class ReceiptTransaction(
    val brand: String?,
    val authCode: String,
    val nsu: String,
)

/** Only an approved purchase offers its tickets (BR-34, AC-33); declined, cancelled, loading and error do not. */
internal fun ReceiptUiState.offersTickets(): Boolean = this is ReceiptUiState.Approved
