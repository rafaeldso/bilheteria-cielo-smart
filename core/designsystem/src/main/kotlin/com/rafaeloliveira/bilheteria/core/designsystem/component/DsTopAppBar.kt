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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

/**
 * Top app bar of the design system: `surface` background, title in `titleLarge` read as a heading.
 * Edge-to-edge: the bar adds the status bar inset itself.
 *
 * Use [DsIconButton] inside [navigationIcon] and [actions] so every icon has a spoken label.
 * The title is limited to two lines because the bar has a fixed height.
 *
 * @param title screen title, already localized.
 * @param modifier applied to the root.
 * @param navigationIcon slot at the start, usually a "back" [DsIconButton]; empty when the screen has no back action.
 * @param actions slot at the end, for icon actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = MAX_TITLE_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

private const val MAX_TITLE_LINES = 2

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsTopAppBarWithoutNavigationPreview() {
    DsTopAppBarPreviewFrame { DsTopAppBar(title = "Eventos à venda") }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsTopAppBarWithActionPreview() {
    DsTopAppBarPreviewFrame {
        DsTopAppBar(
            title = "Eventos à venda",
            actions = {
                DsIconButton(icon = ComponentIcons.Add, contentDescription = "Histórico de compras", onClick = {})
            },
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsTopAppBarWithNavigationPreview() {
    DsTopAppBarPreviewFrame {
        DsTopAppBar(
            title = "Comprovante",
            navigationIcon = {
                DsIconButton(
                    icon = ComponentIcons.ArrowBack,
                    contentDescription = "Voltar",
                    onClick = {},
                )
            },
        )
    }
}

@Composable
private fun DsTopAppBarPreviewFrame(content: @Composable () -> Unit) {
    BilheteriaTheme {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}
