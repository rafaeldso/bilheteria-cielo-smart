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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private val IconSize = 18.dp
private val IconTextGap = 8.dp
private val HorizontalPadding = 24.dp
private val HorizontalPaddingWithIcon = 16.dp

/**
 * Button of the design system (Filled, Tonal or Outlined), `shape.full`, `labelLarge`.
 *
 * Tonal uses the Material 3 defaults, which are `secondaryContainer` on `onSecondaryContainer`.
 * Touch target is at least 48dp high (Material 3 adds the extra area around the 40dp container).
 * A disabled button is announced as disabled by TalkBack.
 *
 * @param text visible label; already localized by the caller.
 * @param onClick called on click; not called when [enabled] is false.
 * @param modifier applied to the root.
 * @param variant visual weight; defaults to [DsButtonVariant.Filled].
 * @param enabled false renders the disabled state (12% container, 38% content) and ignores clicks.
 * @param fillMaxWidth true makes the button as wide as its parent (the `.btn.block` of the kit).
 * @param leadingIcon optional 18dp decorative icon before the label; the label already describes the action.
 */
@Composable
fun DsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DsButtonVariant = DsButtonVariant.Filled,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val buttonModifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier
    val contentPadding =
        PaddingValues(
            horizontal = if (leadingIcon != null) HorizontalPaddingWithIcon else HorizontalPadding,
        )
    val content: @Composable () -> Unit = { DsButtonContent(text = text, leadingIcon = leadingIcon) }

    when (variant) {
        DsButtonVariant.Filled -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                contentPadding = contentPadding,
            ) { content() }
        }

        DsButtonVariant.Tonal -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                contentPadding = contentPadding,
            ) { content() }
        }

        DsButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                contentPadding = contentPadding,
                border = outlinedBorder(enabled),
            ) { content() }
        }
    }
}

@Composable
private fun outlinedBorder(enabled: Boolean): BorderStroke {
    val colors = MaterialTheme.colorScheme
    val color = if (enabled) colors.outline else colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    return BorderStroke(width = 1.dp, color = color)
}

@Composable
private fun DsButtonContent(
    text: String,
    leadingIcon: ImageVector?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(IconTextGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(IconSize))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private const val DISABLED_CONTAINER_ALPHA = 0.12f

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledPreview() {
    DsButtonPreviewFrame { DsButton(text = "Pagar R$ 100,00 no terminal", onClick = {}) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledWithIconPreview() {
    DsButtonPreviewFrame { DsButton(text = "Nova venda", onClick = {}, leadingIcon = ComponentIcons.Add) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledFullWidthPreview() {
    DsButtonPreviewFrame { DsButton(text = "Nova venda", onClick = {}, fillMaxWidth = true) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledDisabledPreview() {
    DsButtonPreviewFrame { DsButton(text = "Pagar R$ 100,00", onClick = {}, enabled = false, fillMaxWidth = true) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonTonalPreview() {
    DsButtonPreviewFrame { DsButton(text = "Atualizar", onClick = {}, variant = DsButtonVariant.Tonal) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonTonalDisabledPreview() {
    DsButtonPreviewFrame {
        DsButton(text = "Atualizar", onClick = {}, variant = DsButtonVariant.Tonal, enabled = false)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonOutlinedPreview() {
    DsButtonPreviewFrame { DsButton(text = "Cancelar", onClick = {}, variant = DsButtonVariant.Outlined) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonOutlinedDisabledPreview() {
    DsButtonPreviewFrame {
        DsButton(text = "Cancelar", onClick = {}, variant = DsButtonVariant.Outlined, enabled = false)
    }
}

@Composable
private fun DsButtonPreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.width(360.dp).padding(16.dp)) { content() }
        }
    }
}
