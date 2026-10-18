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

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsKeyValue
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.SaleResult
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenDateTimeText
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenMoneyText

// Resource lookup of the receipt: the pure model (ReceiptRows) decides what is shown, these functions say in which
// words. Kept apart so the model stays testable on the JVM without resources.

@StringRes
internal fun SaleResult.titleRes(): Int =
    when (this) {
        SaleResult.Approved -> R.string.ticketing_receipt_approved_title
        SaleResult.Declined -> R.string.ticketing_receipt_declined_title
        SaleResult.Cancelled -> R.string.ticketing_receipt_cancelled_title
    }

@StringRes
internal fun SaleResult.bodyRes(): Int =
    when (this) {
        SaleResult.Approved -> R.string.ticketing_receipt_approved_body
        SaleResult.Declined -> R.string.ticketing_receipt_declined_body
        SaleResult.Cancelled -> R.string.ticketing_receipt_cancelled_body
    }

/**
 * The rows as `DsKeyValueList` items, built again only when the rows or the resources change, not at every
 * recomposition (the strings do not depend on anything else).
 */
@Composable
internal fun rememberKeyValues(rows: List<ReceiptRow>): List<DsKeyValue> {
    val resources = LocalResources.current
    return remember(rows, resources) { rows.map { it.toKeyValue(resources) } }
}

/**
 * One row for `DsKeyValueList`. Amounts and dates also carry the text TalkBack reads ("Total, 100 reais";
 * "Data do evento, 15 de novembro às 20:00").
 */
internal fun ReceiptRow.toKeyValue(resources: Resources): DsKeyValue {
    val label = resources.getString(field.labelRes)
    return when (val content = value) {
        is ReceiptValue.Text -> {
            DsKeyValue(label = label, value = content.text)
        }

        is ReceiptValue.Tickets -> {
            DsKeyValue(
                label = label,
                value = resources.getQuantityString(R.plurals.ticketing_receipt_tickets, content.count, content.count),
            )
        }

        is ReceiptValue.Amount -> {
            DsKeyValue(
                label = label,
                value = content.formatted,
                spokenValue = resources.spokenMoneyText(content.spoken),
            )
        }

        is ReceiptValue.Moment -> {
            DsKeyValue(
                label = label,
                value =
                    resources.getString(
                        R.string.ticketing_receipt_datetime_format,
                        content.dateTime.date,
                        content.dateTime.time,
                    ),
                spokenValue = resources.spokenDateTimeText(content.spoken),
            )
        }

        is ReceiptValue.Result -> {
            DsKeyValue(label = label, value = resources.getString(content.outcome.statusRes))
        }
    }
}
