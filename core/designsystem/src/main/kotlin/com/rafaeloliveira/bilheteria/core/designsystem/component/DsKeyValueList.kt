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
package com.rafaeloliveira.bilheteria.core.designsystem.component

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular

private val RowVerticalPadding = 14.dp

/**
 * Read-only list of label/value pairs (e.g. the fields of a receipt) on a `surfaceContainerLow` card with
 * `shape.medium`, rows separated by an `outlineVariant` line.
 *
 * In each row the label (`bodyMedium`, `onSurfaceVariant`) is at the start and the value (`bodyMedium`,
 * `onSurface`, tabular digits) at the end. When both do not fit on a line (large fonts), the value goes
 * below the label, so nothing is truncated. Each row is one node that TalkBack reads as a pair
 * ("Nº da compra, 7F3K-9Q2L"), or "label, spokenValue" when the item has a [DsKeyValue.spokenValue];
 * the list itself has no extra semantics.
 *
 * @param items rows in display order; an empty list draws nothing visible.
 * @param modifier applied to the root; the list fills the available width.
 */
@Composable
fun DsKeyValueList(
    items: List<DsKeyValue>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                KeyValueRow(item = item)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyValueRow(item: DsKeyValue) {
    val spacing = BilheteriaTheme.spacing
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .rowSemantics(item)
                .padding(horizontal = spacing.lg, vertical = RowVerticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = spacing.lg),
        )
        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private fun Modifier.rowSemantics(item: DsKeyValue): Modifier =
    if (item.spokenValue != null) {
        clearAndSetSemantics { contentDescription = "${item.label}, ${item.spokenValue}" }
    } else {
        semantics(mergeDescendants = true) {}
    }

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsKeyValueListPreview() {
    DsPreviewSurface { DsKeyValueList(items = previewReceiptFields()) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsKeyValueListSingleRowPreview() {
    DsPreviewSurface {
        DsKeyValueList(items = listOf(DsKeyValue(label = "Nº da compra", value = "7F3K-9Q2L")))
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsKeyValueListLargeFontPreview() {
    DsPreviewSurface { DsKeyValueList(items = previewReceiptFields()) }
}

private fun previewReceiptFields(): List<DsKeyValue> =
    listOf(
        DsKeyValue(label = "Nº da compra", value = "7F3K-9Q2L"),
        DsKeyValue(label = "Evento", value = "Noite de Jazz no Parque"),
        DsKeyValue(label = "Quantidade", value = "2"),
        DsKeyValue(label = "Total", value = "R$ 100,00", spokenValue = "100 reais"),
        DsKeyValue(label = "Data/hora da compra", value = "17/10/2026 às 14:32"),
    )
