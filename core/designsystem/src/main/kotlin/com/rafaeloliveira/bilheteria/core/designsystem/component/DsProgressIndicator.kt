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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** While motion is reduced the bar is still: the whole track is filled at half opacity (the kit's `.progress`). */
private const val STATIC_ALPHA = 0.5f

/**
 * Indeterminate linear progress for waiting on an external event of unknown duration (the payment
 * result, a status lookup). 4dp, `primary` indicator on a `secondaryContainer` track, full width.
 *
 * It stays still when the user removed animations in the system settings: a full bar at half opacity. In both
 * cases TalkBack reads it as an indeterminate progress bar labeled by [contentDescription].
 *
 * @param contentDescription what is being awaited, already localized (e.g. "Aguardando o pagamento").
 * @param modifier applied to the root.
 */
@Composable
fun DsProgressIndicator(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val semanticsModifier =
        modifier.fillMaxWidth().clearAndSetSemantics {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
    if (rememberReducedMotion()) {
        LinearProgressIndicator(
            progress = { 1f },
            modifier = semanticsModifier,
            color = colors.primary.copy(alpha = STATIC_ALPHA),
            trackColor = colors.secondaryContainer,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    } else {
        LinearProgressIndicator(
            modifier = semanticsModifier,
            color = colors.primary,
            trackColor = colors.secondaryContainer,
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsProgressIndicatorPreview() {
    DsPreviewSurface { DsProgressIndicator(contentDescription = "Aguardando o pagamento") }
}
