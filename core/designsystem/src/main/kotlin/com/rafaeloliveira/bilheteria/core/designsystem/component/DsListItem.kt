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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.Spacing
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular

private val ItemMinHeight = 72.dp
private val LeadingIconSize = 20.dp

/** From this font scale the trailing content moves below the texts: side by side the text column gets too narrow. */
private const val STACKED_TRAILING_FONT_SCALE = 1.5f

/**
 * List row of the design system: optional 40dp circular leading icon (`secondaryContainer`), a headline in
 * `labelLarge`, an optional supporting line in `bodyMedium` and an optional trailing column aligned to the end.
 *
 * Headline and supporting line wrap up to two lines each; the trailing content never truncates. With font scale
 * of 1.5 or more the trailing content moves below the texts (start aligned with them), so the text column keeps
 * the full width instead of being squeezed. The whole row is one node: when [onClick] is set it is clickable as a
 * button (ripple included) and its descendants are merged, read by TalkBack as a single item.
 *
 * @param headline main text (e.g. event name), already localized.
 * @param modifier applied to the root.
 * @param supportingText second line (e.g. date, venue); null shows only the headline.
 * @param leadingIcon decorative icon inside the leading circle; null omits the circle.
 * @param onClick called on click; null makes the row non-interactive.
 * @param spokenLabel full sentence TalkBack reads for the row, with dates and amounts spelled out; null reads
 * the merged visible texts.
 * @param showTopDivider true draws an `outlineVariant` line above the row, to separate it from the previous one.
 * @param trailing slot at the end for the value and a second line (price and availability, total and badge);
 * its content is aligned to the end; null omits it.
 */
@Composable
fun DsListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    spokenLabel: String? = null,
    showTopDivider: Boolean = false,
    trailing: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val rowModifier = itemRowModifier(BilheteriaTheme.spacing, onClick, spokenLabel)
    val stackTrailing = trailing != null && LocalDensity.current.fontScale >= STACKED_TRAILING_FONT_SCALE
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (stackTrailing && trailing != null) {
            StackedItem(headline, supportingText, leadingIcon, trailing, rowModifier)
        } else {
            SideBySideItem(headline, supportingText, leadingIcon, trailing, rowModifier)
        }
    }
}

@Composable
private fun SideBySideItem(
    headline: String,
    supportingText: String?,
    leadingIcon: ImageVector?,
    trailing: (@Composable ColumnScope.() -> Unit)?,
    modifier: Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            LeadingCircle(icon = leadingIcon)
        }
        ItemTexts(headline = headline, supportingText = supportingText, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                horizontalAlignment = Alignment.End,
                content = trailing,
            )
        }
    }
}

@Composable
private fun StackedItem(
    headline: String,
    supportingText: String?,
    leadingIcon: ImageVector?,
    trailing: @Composable ColumnScope.() -> Unit,
    modifier: Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    val trailingStart = if (leadingIcon != null) BilheteriaTheme.sizes.iconLarge + spacing.lg else 0.dp
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                LeadingCircle(icon = leadingIcon)
            }
            ItemTexts(headline = headline, supportingText = supportingText, modifier = Modifier.weight(1f))
        }
        Column(
            modifier = Modifier.padding(start = trailingStart),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            horizontalAlignment = Alignment.Start,
            content = trailing,
        )
    }
}

private fun itemRowModifier(
    spacing: Spacing,
    onClick: (() -> Unit)?,
    spokenLabel: String?,
): Modifier =
    Modifier
        .fillMaxWidth()
        .heightIn(min = ItemMinHeight)
        .itemClickable(onClick)
        .semantics(mergeDescendants = true) {
            if (spokenLabel != null) contentDescription = spokenLabel
        }.padding(
            start = spacing.lg,
            top = spacing.sm,
            end = spacing.xl,
            bottom = spacing.sm,
        )

private fun Modifier.itemClickable(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) clickable(role = Role.Button, onClick = onClick) else this

@Composable
private fun LeadingCircle(icon: ImageVector) {
    Box(
        modifier =
            Modifier
                .size(BilheteriaTheme.sizes.iconLarge)
                .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(LeadingIconSize),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ItemTexts(
    headline: String,
    supportingText: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = headline,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = MAX_TEXT_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = MAX_TEXT_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val MAX_TEXT_LINES = 2

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsListItemHeadlineOnlyPreview() {
    DsPreviewSurface(padded = false) { DsListItem(headline = "Noite de Jazz no Parque") }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsListItemTwoLinesPreview() {
    DsPreviewSurface(padded = false) {
        DsListItem(
            headline = "Noite de Jazz no Parque",
            supportingText = "sáb., 15 nov · 20:00 · Concha Acústica Municipal",
            onClick = {},
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsListItemEventPreview() {
    DsPreviewSurface(padded = false) {
        DsListItem(
            headline = "Noite de Jazz no Parque",
            supportingText = "sáb., 15 nov · 20:00 · Concha Acústica Municipal",
            leadingIcon = DsIcons.ConfirmationNumber,
            onClick = {},
            spokenLabel = "Noite de Jazz no Parque, sábado, 15 de novembro, às 20 horas, 50 reais por ingresso",
        ) {
            Text(text = "R$ 50,00", style = MaterialTheme.typography.labelLarge.tabular())
            Text(
                text = "48 disponíveis",
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsListItemPurchasePreview() {
    DsPreviewSurface(padded = false) {
        DsListItem(
            headline = "Noite de Jazz no Parque",
            supportingText = "hoje às 14:32 · 2 ingressos",
            leadingIcon = DsIcons.CheckCircle,
            onClick = {},
            showTopDivider = true,
        ) {
            Text(text = "R$ 100,00", style = MaterialTheme.typography.labelLarge.tabular())
            DsStatusBadge(text = "Aprovada", variant = DsStatusBadgeVariant.Success)
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsListItemLongTextPreview() {
    DsPreviewSurface(padded = false) {
        DsListItem(
            headline = "Festival de Música Eletrônica e Artes Visuais do Litoral Norte",
            supportingText = "sáb., 15 nov · 20:00 · Arena Multiuso do Complexo Esportivo Municipal",
            leadingIcon = DsIcons.ConfirmationNumber,
            onClick = {},
        ) {
            Text(text = "R$ 1.250,00", style = MaterialTheme.typography.labelLarge.tabular())
            Text(text = "1.000 disponíveis", style = MaterialTheme.typography.labelMedium.tabular())
        }
    }
}
