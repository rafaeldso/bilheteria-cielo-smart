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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private val BadgeHorizontalPadding = 8.dp
private val BadgeVerticalPadding = 2.dp
private val BadgeIconSize = 14.dp
private val BadgeIconTextGap = 4.dp

/**
 * Status badge: container color of the [variant], a 14dp icon and the [text], in `labelMedium` on `shape.small`.
 * Color, icon and text always go together, so the status is never conveyed by color alone. The badge is read
 * as one node whose spoken text is [text]; the icon is decorative.
 *
 * It is not interactive and wraps its content, so it can sit in a trailing slot or on its own line.
 *
 * @param text status name, already localized (e.g. "Aprovada").
 * @param modifier applied to the root.
 * @param variant meaning, color and icon of the badge; defaults to [DsStatusBadgeVariant.Neutral].
 */
@Composable
fun DsStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: DsStatusBadgeVariant = DsStatusBadgeVariant.Neutral,
) {
    val colors = badgeColors(variant)
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(BadgeIconTextGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = badgeIcon(variant),
                contentDescription = null,
                modifier = Modifier.size(BadgeIconSize),
            )
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private class BadgeColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun badgeColors(variant: DsStatusBadgeVariant): BadgeColors {
    val scheme = MaterialTheme.colorScheme
    val extended = BilheteriaTheme.extendedColors
    return when (variant) {
        DsStatusBadgeVariant.Success -> BadgeColors(extended.successContainer, extended.onSuccessContainer)
        DsStatusBadgeVariant.Error -> BadgeColors(scheme.errorContainer, scheme.onErrorContainer)
        DsStatusBadgeVariant.Neutral -> BadgeColors(scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        DsStatusBadgeVariant.Warning -> BadgeColors(extended.warningContainer, extended.onWarningContainer)
    }
}

private fun badgeIcon(variant: DsStatusBadgeVariant): ImageVector =
    when (variant) {
        DsStatusBadgeVariant.Success -> StatusBadgeIcons.Success
        DsStatusBadgeVariant.Error -> StatusBadgeIcons.Error
        DsStatusBadgeVariant.Neutral -> StatusBadgeIcons.Neutral
        DsStatusBadgeVariant.Warning -> StatusBadgeIcons.Warning
    }

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsStatusBadgeSuccessPreview() {
    DsStatusBadgePreviewFrame { DsStatusBadge(text = "Aprovada", variant = DsStatusBadgeVariant.Success) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsStatusBadgeErrorPreview() {
    DsStatusBadgePreviewFrame { DsStatusBadge(text = "Negada", variant = DsStatusBadgeVariant.Error) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsStatusBadgeNeutralPreview() {
    DsStatusBadgePreviewFrame { DsStatusBadge(text = "Cancelada", variant = DsStatusBadgeVariant.Neutral) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsStatusBadgeWarningPreview() {
    DsStatusBadgePreviewFrame { DsStatusBadge(text = "Pendente", variant = DsStatusBadgeVariant.Warning) }
}

@Composable
private fun DsStatusBadgePreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
