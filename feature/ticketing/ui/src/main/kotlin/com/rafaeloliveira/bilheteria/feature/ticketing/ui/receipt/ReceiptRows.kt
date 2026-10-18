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

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.LiveRegionMode
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusBadgeVariant
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import java.time.ZoneId

/**
 * Result of the sale shown by the receipt. It decides the badge and how the result is announced (ui-spec 4.6):
 * an approval is a status (polite); a decline or cancellation needs the operator's attention (assertive).
 */
internal enum class ReceiptOutcome(
    val badge: DsStatusBadgeVariant,
    val liveRegion: LiveRegionMode,
) {
    Approved(DsStatusBadgeVariant.Success, LiveRegionMode.Polite),
    Declined(DsStatusBadgeVariant.Error, LiveRegionMode.Assertive),
    Cancelled(DsStatusBadgeVariant.Neutral, LiveRegionMode.Assertive),
}

/**
 * Fields of the receipt, in display order (BR-16). There is deliberately no card number field: Cielo never sends
 * it to the app and it is never shown. [Brand], [AuthCode] and [Nsu] exist only for an approved purchase (AC-16).
 */
internal enum class ReceiptField(
    @StringRes val labelRes: Int,
) {
    PurchaseNumber(R.string.ticketing_receipt_field_purchase_number),
    Event(R.string.ticketing_receipt_field_event),
    EventDate(R.string.ticketing_receipt_field_event_date),
    Venue(R.string.ticketing_receipt_field_venue),
    Quantity(R.string.ticketing_receipt_field_quantity),
    UnitPrice(R.string.ticketing_receipt_field_unit_price),
    Total(R.string.ticketing_receipt_field_total),
    PurchasedAt(R.string.ticketing_receipt_field_purchased_at),
    Status(R.string.ticketing_receipt_field_status),
    Brand(R.string.ticketing_receipt_field_brand),
    AuthCode(R.string.ticketing_receipt_field_auth_code),
    Nsu(R.string.ticketing_receipt_field_nsu),
}

/** Value of a row, still without resources: the screen turns it into text with `stringResource`. */
internal sealed interface ReceiptValue {
    data class Text(
        val text: String,
    ) : ReceiptValue

    data class Tickets(
        val count: Int,
    ) : ReceiptValue

    data class Amount(
        val formatted: String,
        val spoken: SpokenMoney,
    ) : ReceiptValue

    data class Moment(
        val dateTime: ReceiptDateTime,
    ) : ReceiptValue

    data class Result(
        val outcome: ReceiptOutcome,
    ) : ReceiptValue
}

internal data class ReceiptRow(
    val field: ReceiptField,
    val value: ReceiptValue,
)

/**
 * Rows of the receipt for [outcome], in the order of BR-16 and the prancha. The transaction rows come from
 * [ReceiptData.transaction], which the ViewModel only fills for an approved purchase (AC-16); the brand row is
 * omitted when Cielo did not send it.
 */
internal fun ReceiptData.toRows(
    outcome: ReceiptOutcome,
    zone: ZoneId,
): List<ReceiptRow> =
    buildList {
        add(ReceiptRow(ReceiptField.PurchaseNumber, ReceiptValue.Text(purchaseNumber)))
        add(ReceiptRow(ReceiptField.Event, ReceiptValue.Text(eventName)))
        add(ReceiptRow(ReceiptField.EventDate, ReceiptValue.Moment(eventStartsAt.toReceiptDateTime(zone))))
        add(ReceiptRow(ReceiptField.Venue, ReceiptValue.Text(venue)))
        add(ReceiptRow(ReceiptField.Quantity, ReceiptValue.Tickets(quantity)))
        add(ReceiptRow(ReceiptField.UnitPrice, unitPrice.toAmount()))
        add(ReceiptRow(ReceiptField.Total, total.toAmount()))
        add(ReceiptRow(ReceiptField.PurchasedAt, ReceiptValue.Moment(purchasedAt.toReceiptDateTime(zone))))
        add(ReceiptRow(ReceiptField.Status, ReceiptValue.Result(outcome)))
        if (outcome == ReceiptOutcome.Approved) transaction?.let { addAll(it.toRows()) }
    }

private fun ReceiptTransaction.toRows(): List<ReceiptRow> =
    buildList {
        brand?.let { add(ReceiptRow(ReceiptField.Brand, ReceiptValue.Text(it))) }
        add(ReceiptRow(ReceiptField.AuthCode, ReceiptValue.Text(authCode)))
        add(ReceiptRow(ReceiptField.Nsu, ReceiptValue.Text(nsu)))
    }

/** Amount ready for the screen: pt-BR text for the eyes and its parts for the screen reader. */
internal fun Money.toAmount(): ReceiptValue.Amount =
    ReceiptValue.Amount(formatted = formatForReceipt(), spoken = toSpokenMoney())
