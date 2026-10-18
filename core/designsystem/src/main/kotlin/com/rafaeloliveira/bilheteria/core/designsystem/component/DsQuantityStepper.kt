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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private val ContainerPadding = 4.dp
private val ValueMinWidth = 56.dp
private const val TABULAR_DIGITS = "tnum"

/**
 * Stepper to pick a whole quantity inside limits: minus button, value, plus button, on a
 * `surfaceContainerHigh` container with `shape.full`. One tap changes the value by one; there is no
 * long press. The component does not know the limits: the caller owns the value and says, through
 * [canDecrement] and [canIncrement], which buttons are enabled (a button at its limit renders disabled,
 * 38% `onSurface`, and TalkBack announces it as disabled). Pass both as false to disable the whole stepper.
 *
 * Accessibility: the value is a polite live region read as [valueDescription]. The group label
 * (e.g. "Quantidade de ingressos") is not a parameter: the caller adds it to [modifier] with
 * `Modifier.semantics { contentDescription = ... }`.
 *
 * @param value current quantity, shown in `headlineSmall` with tabular digits.
 * @param onDecrement called on tap of the minus button; not called when [canDecrement] is false.
 * @param onIncrement called on tap of the plus button; not called when [canIncrement] is false.
 * @param canDecrement false disables the minus button (value is at the lower limit).
 * @param canIncrement false disables the plus button (value is at the upper limit).
 * @param valueDescription spoken form of the value, already localized (e.g. "2 ingressos").
 * @param decrementDescription spoken label of the minus button (e.g. "Diminuir quantidade").
 * @param incrementDescription spoken label of the plus button (e.g. "Aumentar quantidade").
 * @param modifier applied to the root.
 */
@Composable
fun DsQuantityStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean,
    valueDescription: String,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .semantics { isTraversalGroup = true }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(ContainerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DsIconButton(
            icon = ComponentIcons.Remove,
            contentDescription = decrementDescription,
            onClick = onDecrement,
            variant = DsIconButtonVariant.Tonal,
            enabled = canDecrement,
        )
        StepperValue(value = value, valueDescription = valueDescription)
        DsIconButton(
            icon = ComponentIcons.Add,
            contentDescription = incrementDescription,
            onClick = onIncrement,
            variant = DsIconButtonVariant.Tonal,
            enabled = canIncrement,
        )
    }
}

@Composable
private fun StepperValue(
    value: Int,
    valueDescription: String,
) {
    val style =
        MaterialTheme.typography.headlineSmall.let { base ->
            remember(base) { base.copy(fontFeatureSettings = TABULAR_DIGITS) }
        }
    Text(
        text = value.toString(),
        modifier =
            Modifier
                .widthIn(min = ValueMinWidth)
                .clearAndSetSemantics {
                    contentDescription = valueDescription
                    liveRegion = LiveRegionMode.Polite
                },
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        style = style,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsQuantityStepperEnabledPreview() {
    DsQuantityStepperPreviewFrame {
        PreviewStepper(value = 2, canDecrement = true, canIncrement = true)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsQuantityStepperAtMinimumPreview() {
    DsQuantityStepperPreviewFrame {
        PreviewStepper(value = 1, canDecrement = false, canIncrement = true)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsQuantityStepperAtMaximumPreview() {
    DsQuantityStepperPreviewFrame {
        PreviewStepper(value = 10, canDecrement = true, canIncrement = false)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsQuantityStepperDisabledPreview() {
    DsQuantityStepperPreviewFrame {
        PreviewStepper(value = 2, canDecrement = false, canIncrement = false)
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsQuantityStepperLargeFontPreview() {
    DsQuantityStepperPreviewFrame {
        PreviewStepper(value = 2, canDecrement = true, canIncrement = true)
    }
}

@Composable
private fun PreviewStepper(
    value: Int,
    canDecrement: Boolean,
    canIncrement: Boolean,
) {
    DsQuantityStepper(
        value = value,
        onDecrement = {},
        onIncrement = {},
        canDecrement = canDecrement,
        canIncrement = canIncrement,
        valueDescription = "$value ingressos",
        decrementDescription = "Diminuir quantidade",
        incrementDescription = "Aumentar quantidade",
    )
}

@Composable
private fun DsQuantityStepperPreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}
