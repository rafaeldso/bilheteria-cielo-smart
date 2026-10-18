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
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsKeyValue
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R

// Resource lookup of the receipt: the pure model (ReceiptRows) decides what is shown, these functions say in which
// words. Kept apart so the model stays testable on the JVM without resources.

@StringRes
internal fun ReceiptOutcome.statusRes(): Int =
    when (this) {
        ReceiptOutcome.Approved -> R.string.ticketing_receipt_status_approved
        ReceiptOutcome.Declined -> R.string.ticketing_receipt_status_declined
        ReceiptOutcome.Cancelled -> R.string.ticketing_receipt_status_cancelled
    }

@StringRes
internal fun ReceiptOutcome.titleRes(): Int =
    when (this) {
        ReceiptOutcome.Approved -> R.string.ticketing_receipt_approved_title
        ReceiptOutcome.Declined -> R.string.ticketing_receipt_declined_title
        ReceiptOutcome.Cancelled -> R.string.ticketing_receipt_cancelled_title
    }

@StringRes
internal fun ReceiptOutcome.bodyRes(): Int =
    when (this) {
        ReceiptOutcome.Approved -> R.string.ticketing_receipt_approved_body
        ReceiptOutcome.Declined -> R.string.ticketing_receipt_declined_body
        ReceiptOutcome.Cancelled -> R.string.ticketing_receipt_cancelled_body
    }

/** One row for `DsKeyValueList`; amounts also carry the text TalkBack reads ("Total, 100 reais"). */
@Composable
internal fun ReceiptRow.toKeyValue(): DsKeyValue {
    val label = stringResource(field.labelRes)
    return when (val content = value) {
        is ReceiptValue.Text -> {
            DsKeyValue(label = label, value = content.text)
        }

        is ReceiptValue.Tickets -> {
            DsKeyValue(
                label = label,
                value = pluralStringResource(R.plurals.ticketing_receipt_tickets, content.count, content.count),
            )
        }

        is ReceiptValue.Amount -> {
            DsKeyValue(label = label, value = content.formatted, spokenValue = content.spoken.asSpokenText())
        }

        is ReceiptValue.Moment -> {
            DsKeyValue(
                label = label,
                value =
                    stringResource(
                        R.string.ticketing_receipt_datetime_format,
                        content.dateTime.date,
                        content.dateTime.time,
                    ),
            )
        }

        is ReceiptValue.Result -> {
            DsKeyValue(label = label, value = stringResource(content.outcome.statusRes()))
        }
    }
}

/** "100 reais", "10 reais e 50 centavos" or "50 centavos": the words come from plural resources. */
@Composable
internal fun SpokenMoney.asSpokenText(): String {
    val reaisText = pluralStringResource(R.plurals.ticketing_receipt_spoken_reais, reaisQuantity, reais)
    val centavosText = pluralStringResource(R.plurals.ticketing_receipt_spoken_centavos, centavos, centavos)
    return when {
        hasReaisAndCentavos -> {
            stringResource(
                R.string.ticketing_receipt_spoken_reais_and_centavos,
                reaisText,
                centavosText,
            )
        }

        onlyCentavos -> {
            centavosText
        }

        else -> {
            reaisText
        }
    }
}
