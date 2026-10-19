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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

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
 * @param busyDescription when not null the button is **busy** (UI Spec 002 §4.3, `.btn.busy`): it keeps the container
 *   and content colors of its variant, shows a small progress ring instead of the icon, ignores clicks and is announced
 *   as disabled with this text as its state (e.g. "Imprimindo os ingressos"). Unlike [enabled] = false it does not dim
 *   to 38%, because the label ("Imprimindo…") is the only progress information on screen.
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
    busyDescription: String? = null,
) {
    val busy = busyDescription != null
    val buttonModifier = modifier.buttonSize(fillMaxWidth).busySemantics(busyDescription)
    val onClickOrNothing = if (busy) ({}) else onClick
    val spacing = BilheteriaTheme.spacing
    val contentPadding = PaddingValues(horizontal = if (leadingIcon != null) spacing.lg else spacing.xl)
    val content: @Composable () -> Unit = { DsButtonContent(text = text, leadingIcon = leadingIcon, busy = busy) }

    when (variant) {
        DsButtonVariant.Filled -> {
            Button(
                onClick = onClickOrNothing,
                modifier = buttonModifier,
                enabled = enabled,
                contentPadding = contentPadding,
            ) { content() }
        }

        DsButtonVariant.Tonal -> {
            FilledTonalButton(
                onClick = onClickOrNothing,
                modifier = buttonModifier,
                enabled = enabled,
                contentPadding = contentPadding,
            ) { content() }
        }

        DsButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClickOrNothing,
                modifier = buttonModifier,
                enabled = enabled,
                contentPadding = contentPadding,
                border = outlinedBorder(enabled),
            ) { content() }
        }
    }
}

private fun Modifier.buttonSize(fillMaxWidth: Boolean): Modifier = if (fillMaxWidth) fillMaxWidth() else this

/** Announces a busy button as disabled with [description] as its state; no change when [description] is null. */
private fun Modifier.busySemantics(description: String?): Modifier =
    if (description == null) {
        this
    } else {
        semantics {
            stateDescription = description
            disabled()
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
    busy: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(
                progress = { BUSY_PROGRESS },
                modifier = Modifier.size(BilheteriaTheme.sizes.iconSmall),
                color = LocalContentColor.current,
                strokeWidth = BUSY_STROKE,
            )
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(BilheteriaTheme.sizes.iconSmall),
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val BUSY_PROGRESS = 0.75f
private val BUSY_STROKE = 2.dp

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) { DsButton(text = "Pagar R$ 100,00 no terminal", onClick = {}) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledWithIconPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(text = "Nova venda", onClick = {}, leadingIcon = DsIcons.AddShoppingCart)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledFullWidthPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) { DsButton(text = "Nova venda", onClick = {}, fillMaxWidth = true) }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledDisabledPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(
            text = "Aguardando pagamento…",
            onClick = {},
            enabled = false,
            fillMaxWidth = true,
            leadingIcon = DsIcons.HourglassTop,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonFilledBusyPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(text = "Imprimindo…", onClick = {}, fillMaxWidth = true, busyDescription = "Imprimindo os ingressos")
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonTonalPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(text = "Atualizar", onClick = {}, variant = DsButtonVariant.Tonal)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonTonalDisabledPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(text = "Atualizar", onClick = {}, variant = DsButtonVariant.Tonal, enabled = false)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonOutlinedPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(text = "Cancelar", onClick = {}, variant = DsButtonVariant.Outlined)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsButtonOutlinedDisabledPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(text = "Cancelar", onClick = {}, variant = DsButtonVariant.Outlined, enabled = false)
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsButtonLargeFontPreview() {
    DsPreviewSurface(Modifier.width(PREVIEW_WIDTH)) {
        DsButton(
            text = "Pagar R$ 100,00 no terminal",
            onClick = {},
            fillMaxWidth = true,
            leadingIcon = DsIcons.PointOfSale,
        )
    }
}

private val PREVIEW_WIDTH = 360.dp
