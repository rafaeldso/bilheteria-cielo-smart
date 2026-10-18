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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

/**
 * Single or multiple selection filter chip: `outline` border when unselected, `secondaryContainer`
 * with a check icon when selected. `shape.small`, `labelLarge`, 48dp touch target.
 * TalkBack announces the selected state.
 *
 * @param label visible text, already localized.
 * @param selected whether the filter is applied.
 * @param onClick called on click, to toggle or select; selection state is owned by the caller.
 * @param modifier applied to the root.
 * @param enabled false renders the disabled state and ignores clicks.
 * @param spokenLabel text TalkBack reads instead of [label] when the visible text is abbreviated
 * (e.g. "dezembro de 2026" for "Dez"); null reads [label].
 */
@Composable
fun DsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    spokenLabel: String? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { ChipLabel(label = label, spokenLabel = spokenLabel) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (selected) ({ SelectedCheck() }) else null,
        shape = MaterialTheme.shapes.small,
        colors = chipColors(),
        border = chipBorder(enabled = enabled, selected = selected),
    )
}

@Composable
private fun ChipLabel(
    label: String,
    spokenLabel: String?,
) {
    val labelModifier =
        if (spokenLabel != null) {
            Modifier.clearAndSetSemantics {
                contentDescription = spokenLabel
                text = AnnotatedString(spokenLabel)
            }
        } else {
            Modifier
        }
    Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = labelModifier)
}

@Composable
private fun SelectedCheck() {
    Icon(
        imageVector = ComponentIcons.Check,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Composable
private fun chipColors(): SelectableChipColors {
    val colors = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = colors.onSurfaceVariant,
        iconColor = colors.onSurfaceVariant,
        selectedContainerColor = colors.secondaryContainer,
        selectedLabelColor = colors.onSecondaryContainer,
        selectedLeadingIconColor = colors.onSecondaryContainer,
    )
}

@Composable
private fun chipBorder(
    enabled: Boolean,
    selected: Boolean,
): BorderStroke? =
    FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = MaterialTheme.colorScheme.outline,
        selectedBorderColor = Color.Transparent,
    )

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsFilterChipUnselectedPreview() {
    DsFilterChipPreviewFrame { DsFilterChip(label = "Dez", selected = false, onClick = {}) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsFilterChipSelectedPreview() {
    DsFilterChipPreviewFrame { DsFilterChip(label = "Todos", selected = true, onClick = {}) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsFilterChipDisabledPreview() {
    DsFilterChipPreviewFrame { DsFilterChip(label = "Jan 2027", selected = false, onClick = {}, enabled = false) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsFilterChipGroupPreview() {
    DsFilterChipPreviewFrame {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DsFilterChip(label = "Todos", selected = true, onClick = {})
            DsFilterChip(label = "Nov", selected = false, onClick = {}, spokenLabel = "novembro de 2026")
            DsFilterChip(label = "Dez", selected = false, onClick = {}, spokenLabel = "dezembro de 2026")
        }
    }
}

@Composable
private fun DsFilterChipPreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
