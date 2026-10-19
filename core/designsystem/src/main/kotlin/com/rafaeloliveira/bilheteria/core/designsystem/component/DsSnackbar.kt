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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Snackbar of the design system (`inverseSurface` on `inverseOnSurface`, action in `inversePrimary`), for failures
 * and confirmations that do not block the screen. It is a plain composable: the caller places it (for example above a
 * bottom bar) and decides how long it stays, usually from its own state.
 *
 * It is announced politely as a live region and never takes focus. The action, when present, is a 48dp text button.
 *
 * @param message already localized.
 * @param modifier applied to the root.
 * @param actionLabel label of the optional action (e.g. "Tentar novamente"); without it there is no action.
 * @param onAction called when the action is clicked.
 */
@Composable
fun DsSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Snackbar(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        containerColor = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        actionContentColor = colors.inversePrimary,
        action =
            actionLabel?.let { label ->
                { TextButton(onClick = onAction) { Text(text = label, style = MaterialTheme.typography.labelLarge) } }
            },
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsSnackbarWithActionPreview() {
    DsPreviewSurface(Modifier.width(SNACKBAR_PREVIEW_WIDTH)) {
        DsSnackbar(
            message = "A impressora está sem papel. Coloque papel e tente de novo.",
            actionLabel = "Tentar novamente",
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsSnackbarPreview() {
    DsPreviewSurface(Modifier.width(SNACKBAR_PREVIEW_WIDTH)) { DsSnackbar(message = "Ingressos impressos") }
}

private val SNACKBAR_PREVIEW_WIDTH = 360.dp
