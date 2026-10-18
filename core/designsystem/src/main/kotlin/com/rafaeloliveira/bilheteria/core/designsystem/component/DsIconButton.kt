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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private val ContainerSize = 48.dp
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * Icon button of the design system, 48dp, always with a spoken label.
 *
 * @param icon the 24dp icon (outlined Material Symbol).
 * @param contentDescription what the action does, already localized; required because the button has no text.
 * @param onClick called on click; not called when [enabled] is false.
 * @param modifier applied to the root.
 * @param variant [DsIconButtonVariant.Standard] or [DsIconButtonVariant.Tonal].
 * @param enabled false renders the disabled state (38% `onSurface`, no container) and ignores clicks.
 */
@Composable
fun DsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DsIconButtonVariant = DsIconButtonVariant.Standard,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val disabledContent = colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    val buttonModifier = modifier.size(ContainerSize)
    when (variant) {
        DsIconButtonVariant.Standard -> {
            IconButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = colors.onSurfaceVariant,
                        disabledContentColor = disabledContent,
                    ),
            ) {
                Icon(imageVector = icon, contentDescription = contentDescription)
            }
        }

        DsIconButtonVariant.Tonal -> {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                colors =
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = disabledContent,
                    ),
            ) {
                Icon(imageVector = icon, contentDescription = contentDescription)
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsIconButtonStandardPreview() {
    DsIconButtonPreviewFrame {
        DsIconButton(icon = ComponentIcons.ArrowBack, contentDescription = "Voltar", onClick = {})
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsIconButtonStandardDisabledPreview() {
    DsIconButtonPreviewFrame {
        DsIconButton(
            icon = ComponentIcons.ArrowBack,
            contentDescription = "Voltar",
            onClick = {},
            enabled = false,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsIconButtonTonalPreview() {
    DsIconButtonPreviewFrame {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DsIconButton(
                icon = ComponentIcons.Remove,
                contentDescription = "Menos um ingresso",
                onClick = {},
                variant = DsIconButtonVariant.Tonal,
            )
            DsIconButton(
                icon = ComponentIcons.Add,
                contentDescription = "Mais um ingresso",
                onClick = {},
                variant = DsIconButtonVariant.Tonal,
            )
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsIconButtonTonalDisabledPreview() {
    DsIconButtonPreviewFrame {
        DsIconButton(
            icon = ComponentIcons.Remove,
            contentDescription = "Menos um ingresso",
            onClick = {},
            variant = DsIconButtonVariant.Tonal,
            enabled = false,
        )
    }
}

@Composable
private fun DsIconButtonPreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
