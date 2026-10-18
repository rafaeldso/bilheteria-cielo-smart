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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

/**
 * Top app bar of the design system: `surface` background, title in `titleLarge` read as a heading.
 * Edge-to-edge: the bar adds the status bar inset itself.
 *
 * Use [DsIconButton] inside [navigationIcon] and [actions] so every icon has a spoken label.
 * The bar is at least 64dp tall and grows with the font scale, so the title (up to two lines) is never
 * cut at 200% font (compose-ui.md, "Texto").
 *
 * @param title screen title, already localized.
 * @param modifier applied to the root.
 * @param navigationIcon slot at the start, usually a "back" [DsIconButton]; empty when the screen has no back action.
 * @param actions slot at the end, for icon actions.
 */
@Composable
fun DsTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val spacing = BilheteriaTheme.spacing
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .heightIn(min = BAR_MIN_HEIGHT)
                    .padding(horizontal = spacing.xs, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                navigationIcon()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = MAX_TITLE_LINES,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.md)
                        .semantics { heading() },
            )
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
    }
}

private val BAR_MIN_HEIGHT = 64.dp
private const val MAX_TITLE_LINES = 2

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsTopAppBarWithoutNavigationPreview() {
    DsPreviewSurface(padded = false) { DsTopAppBar(title = "Eventos à venda") }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsTopAppBarWithActionPreview() {
    DsPreviewSurface(padded = false) {
        DsTopAppBar(
            title = "Eventos à venda",
            actions = {
                DsIconButton(icon = DsIcons.ReceiptLong, contentDescription = "Histórico de compras", onClick = {})
            },
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsTopAppBarWithNavigationPreview() {
    DsPreviewSurface(padded = false) {
        DsTopAppBar(
            title = "Comprovante",
            navigationIcon = {
                DsIconButton(
                    icon = DsIcons.ArrowBack,
                    contentDescription = "Voltar",
                    onClick = {},
                )
            },
        )
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsTopAppBarLargeFontPreview() {
    DsPreviewSurface(padded = false) {
        DsTopAppBar(
            title = "Eventos à venda",
            actions = {
                DsIconButton(icon = DsIcons.ReceiptLong, contentDescription = "Histórico de compras", onClick = {})
            },
        )
    }
}

@Preview(name = "Font 2x · long title", fontScale = 2f)
@Composable
private fun DsTopAppBarLargeFontLongTitlePreview() {
    DsPreviewSurface(padded = false) {
        DsTopAppBar(
            title = "Histórico de compras",
            navigationIcon = {
                DsIconButton(icon = DsIcons.ArrowBack, contentDescription = "Voltar", onClick = {})
            },
        )
    }
}
