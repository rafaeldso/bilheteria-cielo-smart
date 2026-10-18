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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private const val ICON_VIEWPORT = 24f
private const val ERROR_PATH =
    "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2z" +
        "m0-4h-2V7h2v6z"
private const val REFRESH_PATH =
    "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08" +
        "c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"

/** Generic failure glyph (Material `error`), used when the caller does not pass an icon. */
private val ErrorIcon: ImageVector by lazy { glyph(name = "Error", path = ERROR_PATH) }

/** Retry glyph (Material `refresh`) shown before the retry label. */
private val RefreshIcon: ImageVector by lazy { glyph(name = "Refresh", path = REFRESH_PATH) }

private fun glyph(
    name: String,
    path: String,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
        ).addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()

/**
 * Error state with a retry action. A 72dp round glyph on `errorContainer`, a heading title, a message
 * that says what happened and what to do (no technical code) and a filled retry button.
 *
 * It is announced to TalkBack as an alert (assertive live region) and moves focus to the retry button
 * when it enters the composition.
 *
 * @param title what failed, already localized (e.g. "Não foi possível carregar os eventos"); a heading.
 * @param body what happened and what to do next; already localized, never an error code or stack trace.
 * @param retryLabel label of the retry button (e.g. "Tentar de novo"); already localized.
 * @param onRetryClick called when the retry button is clicked.
 * @param modifier applied to the root.
 * @param icon 32dp decorative glyph; defaults to a generic error mark.
 */
@Composable
fun DsErrorState(
    title: String,
    body: String,
    retryLabel: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = ErrorIcon,
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
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        action = {
            DsButton(
                text = retryLabel,
                onClick = onRetryClick,
                modifier = Modifier.focusRequester(retryFocus),
                leadingIcon = RefreshIcon,
            )
        },
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsErrorStatePreview() {
    DsErrorStatePreviewFrame {
        DsErrorState(
            title = "Não foi possível carregar os eventos",
            body = "Os dados dos eventos não puderam ser lidos. Tente de novo.",
            retryLabel = "Tentar de novo",
            onRetryClick = {},
        )
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsErrorStateLargeFontPreview() {
    DsErrorStatePreviewFrame {
        DsErrorState(
            title = "Não foi possível carregar o histórico",
            body = "As compras registradas não puderam ser lidas. Tente de novo.",
            retryLabel = "Tentar de novo",
            onRetryClick = {},
        )
    }
}

@Composable
private fun DsErrorStatePreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            content()
        }
    }
}
