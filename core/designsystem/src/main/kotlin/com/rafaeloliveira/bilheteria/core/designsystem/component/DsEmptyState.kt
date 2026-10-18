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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons

/**
 * Empty state: a successful load that returned nothing (not an error). A 72dp round glyph on
 * `surfaceContainerHigh`, a title read as a heading, an explanation and an optional action.
 *
 * @param title what is empty, already localized (e.g. "Nenhum evento à venda"); read as a heading.
 * @param body what will appear here and when; already localized.
 * @param modifier applied to the root.
 * @param icon 32dp decorative glyph; defaults to a generic inbox.
 * @param action optional action below the text (typically a tonal `DsButton`); null renders none.
 */
@Composable
fun DsEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = DsIcons.Inbox,
    action: (@Composable () -> Unit)? = null,
) {
    StateMessage(
        icon = icon,
        glyphContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        glyphContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        body = body,
        modifier = modifier,
        action = action,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsEmptyStatePreview() {
    DsPreviewSurface(padded = false) {
        DsEmptyState(
            title = "Nenhum evento à venda",
            body = "Eventos com data futura e ingressos disponíveis aparecem aqui.",
            icon = DsIcons.Event,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsEmptyStateWithActionPreview() {
    DsPreviewSurface(padded = false) {
        DsEmptyState(
            title = "Nenhuma compra registrada",
            body = "Vendas aprovadas, negadas ou canceladas aparecem aqui.",
            icon = DsIcons.ReceiptLong,
            action = { DsButton(text = "Atualizar", onClick = {}, variant = DsButtonVariant.Tonal) },
        )
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsEmptyStateLargeFontPreview() {
    DsPreviewSurface(padded = false) {
        DsEmptyState(
            title = "Nenhum evento à venda",
            body = "Eventos com data futura e ingressos disponíveis aparecem aqui.",
            icon = DsIcons.Event,
        )
    }
}
