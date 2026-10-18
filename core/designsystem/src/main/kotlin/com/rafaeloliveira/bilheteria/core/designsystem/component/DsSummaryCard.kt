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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private val CardPadding = 16.dp
private val CardGap = 4.dp
private const val TABULAR_DIGITS = "tnum"

/**
 * Summary card with the dominant figure of a screen (e.g. the purchase total): `primaryContainer` on
 * `shape.large`, a [label] in `labelLarge`, the [value] in `displaySmall` with tabular digits and an optional
 * [supportingText] in `bodyMedium`. Always fills the available width.
 *
 * The card is read by TalkBack as a single group: the merged texts, or [spokenLabel] when given.
 *
 * @param label what the value means (e.g. "Total"), already localized.
 * @param value the highlighted figure, already formatted (e.g. "R$ 100,00").
 * @param modifier applied to the root.
 * @param supportingText detail under the value (e.g. "2 ingressos × R$ 50,00"); null omits the line.
 * @param spokenLabel sentence TalkBack reads for the whole card with amounts spelled out (e.g. "Total 100 reais,
 * 2 ingressos de 50 reais"); null reads the visible texts.
 */
@Composable
fun DsSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    spokenLabel: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().summarySemantics(spokenLabel),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TABULAR_DIGITS),
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_DIGITS),
                )
            }
        }
    }
}

private fun Modifier.summarySemantics(spokenLabel: String?): Modifier =
    if (spokenLabel != null) {
        clearAndSetSemantics { contentDescription = spokenLabel }
    } else {
        semantics(mergeDescendants = true) {}
    }

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsSummaryCardWithSupportingTextPreview() {
    DsSummaryCardPreviewFrame {
        DsSummaryCard(
            label = "Total",
            value = "R$ 100,00",
            supportingText = "2 ingressos × R$ 50,00",
            spokenLabel = "Total 100 reais, 2 ingressos de 50 reais",
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsSummaryCardValueOnlyPreview() {
    DsSummaryCardPreviewFrame { DsSummaryCard(label = "Total", value = "R$ 60,00") }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsSummaryCardLargeFontPreview() {
    DsSummaryCardPreviewFrame {
        DsSummaryCard(label = "Total", value = "R$ 1.250,00", supportingText = "25 ingressos × R$ 50,00")
    }
}

@Composable
private fun DsSummaryCardPreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
