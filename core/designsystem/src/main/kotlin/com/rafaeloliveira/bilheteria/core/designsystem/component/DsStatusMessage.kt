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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.tooling.preview.Preview
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

/**
 * Status message for something the user can only wait for (the payment on the terminal, the confirmation with
 * the acquirer): a 72dp round glyph on `surfaceContainerHigh`, a heading title and an explanation, centered.
 * Pair it with a [DsProgressIndicator] below it while the wait lasts.
 *
 * The title and body are announced to TalkBack as one status (polite live region): it does not interrupt
 * what the user is hearing, unlike [DsErrorState].
 *
 * @param icon 32dp decorative glyph that hints at what is awaited (e.g. a terminal, a sync).
 * @param title what is happening, already localized (e.g. "Conclua o pagamento no terminal"); a heading.
 * @param body what to do or what to expect meanwhile; already localized.
 * @param modifier applied to the root.
 * @param supportingContent optional content below the body (e.g. a summary of the purchase); it is not part
 * of the announced status; null renders none.
 */
@Composable
fun DsStatusMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
) {
    StateMessage(
        icon = icon,
        glyphContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        glyphContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        body = body,
        modifier = modifier,
        liveRegion = LiveRegionMode.Polite,
        supportingContent = supportingContent,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsStatusMessageAwaitingPaymentPreview() {
    DsPreviewSurface(padded = false) {
        Column {
            DsStatusMessage(
                icon = DsIcons.PointOfSale,
                title = "Conclua o pagamento no terminal",
                body = "R$ 100,00 · 2 ingressos. Não feche o app até o resultado aparecer.",
            )
            DsProgressIndicator(
                contentDescription = "Aguardando o pagamento",
                modifier = Modifier.padding(horizontal = BilheteriaTheme.spacing.lg),
            )
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsStatusMessageRecoveringPreview() {
    DsPreviewSurface(padded = false) {
        Column {
            DsStatusMessage(
                icon = DsIcons.Sync,
                title = "Confirmando o pagamento com a Cielo",
                body =
                    "Não recebemos uma confirmação clara deste pagamento. Antes de permitir uma nova cobrança, " +
                        "estamos consultando se ele já foi feito.",
            )
            DsProgressIndicator(
                contentDescription = "Consultando a Cielo",
                modifier = Modifier.padding(horizontal = BilheteriaTheme.spacing.lg),
            )
        }
    }
}

@Preview(name = "Font 2x", fontScale = 2f)
@Composable
private fun DsStatusMessageLargeFontPreview() {
    DsPreviewSurface(padded = false) {
        DsStatusMessage(
            icon = DsIcons.PointOfSale,
            title = "Conclua o pagamento no terminal",
            body = "R$ 100,00 · 2 ingressos. Não feche o app até o resultado aparecer.",
        )
    }
}
