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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private val TopPadding = 64.dp
private val GlyphSize = 72.dp
private val GlyphIconSize = 32.dp
private val BodyMaxWidth = 280.dp

/**
 * Layout shared by [DsEmptyState], [DsErrorState] and [DsStatusMessage] (the `.state-msg` of the kit): a 72dp
 * round glyph, a heading title, a body text, an optional supporting slot and an optional action, all centered.
 *
 * The glyph is decorative, so it has no description; the title and body carry the meaning. With a
 * [liveRegion], only the title and body are merged into one announced node: [supportingContent] and [action]
 * stay outside it, so the action keeps its own focus and role.
 */
@Composable
internal fun StateMessage(
    icon: ImageVector,
    glyphContainerColor: Color,
    glyphContentColor: Color,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    liveRegion: LiveRegionMode? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val spacing = BilheteriaTheme.spacing
    Column(
        modifier =
            modifier.fillMaxWidth().padding(
                PaddingValues(start = spacing.xxl, top = TopPadding, end = spacing.xxl, bottom = spacing.xl),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier.size(GlyphSize).background(glyphContainerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(GlyphIconSize),
                tint = glyphContentColor,
            )
        }
        MessageText(title = title, body = body, liveRegion = liveRegion)
        supportingContent?.invoke()
        action?.invoke()
    }
}

@Composable
private fun MessageText(
    title: String,
    body: String,
    liveRegion: LiveRegionMode?,
) {
    val groupModifier =
        if (liveRegion != null) {
            Modifier.semantics(mergeDescendants = true) { this.liveRegion = liveRegion }
        } else {
            Modifier
        }
    Column(
        modifier = groupModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.md),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            modifier = Modifier.widthIn(max = BodyMaxWidth),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
