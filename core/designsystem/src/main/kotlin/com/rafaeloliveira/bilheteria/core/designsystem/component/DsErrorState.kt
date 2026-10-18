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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular

/**
 * Error state with a retry action. A 72dp round glyph on `errorContainer`, a heading title, a message
 * that says what happened and what to do (no technical code) and a filled retry button.
 *
 * The title and body are announced to TalkBack as one alert (assertive live region) and focus moves to the
 * retry button when the state enters the composition. The button is outside the alert, so it keeps its
 * own label and role.
 *
 * @param title what failed, already localized (e.g. "Não foi possível carregar os eventos"); a heading.
 * @param body what happened and what to do next; already localized, never an error code or stack trace.
 * @param retryLabel label of the retry button (e.g. "Tentar de novo"); already localized.
 * @param onRetryClick called when the retry button is clicked.
 * @param modifier applied to the root.
 * @param icon 32dp decorative glyph; defaults to a generic error mark.
 * @param supportingContent optional content between the body and the button (e.g. the quantity and total of the
 * purchase that failed); it is not part of the alert; null renders none.
 */
@Composable
fun DsErrorState(
    title: String,
    body: String,
    retryLabel: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = DsIcons.Error,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    val retryFocus = remember { FocusRequester() }
    if (!LocalInspectionMode.current) {
        LaunchedEffect(retryFocus) { retryFocus.requestFocus() }
    }
    StateMessage(
        icon = icon,
        glyphContainerColor = MaterialTheme.colorScheme.errorContainer,
        glyphContentColor = MaterialTheme.colorScheme.onErrorContainer,
        title = title,
        body = body,
        modifier = modifier,
        liveRegion = LiveRegionMode.Assertive,
        supportingContent = supportingContent,
        action = {
            DsButton(
                text = retryLabel,
                onClick = onRetryClick,
                modifier = Modifier.focusRequester(retryFocus),
                leadingIcon = DsIcons.Refresh,
            )
        },
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsErrorStatePreview() {
    DsPreviewSurface(padded = false) {
        DsErrorState(
            title = "Não foi possível carregar os eventos",
            body = "Os dados dos eventos não puderam ser lidos. Tente de novo.",
            retryLabel = "Tentar de novo",
            onRetryClick = {},
            icon = DsIcons.EventBusy,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsErrorStateWithSupportingContentPreview() {
    DsPreviewSurface(padded = false) {
        DsErrorState(
            title = "Não foi possível concluir o pagamento",
            body = "A Cielo informou um erro e a venda não foi registrada. Você pode tentar de novo.",
            retryLabel = "Tentar pagar R$ 100,00 de novo",
            onRetryClick = {},
            supportingContent = {
                Text(
                    text = "2 ingressos · R$ 100,00",
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsErrorStateLargeFontPreview() {
    DsPreviewSurface(padded = false) {
        DsErrorState(
            title = "Não foi possível carregar o histórico",
            body = "As compras registradas não puderam ser lidas. Tente de novo.",
            retryLabel = "Tentar de novo",
            onRetryClick = {},
            icon = DsIcons.History,
        )
    }
}
