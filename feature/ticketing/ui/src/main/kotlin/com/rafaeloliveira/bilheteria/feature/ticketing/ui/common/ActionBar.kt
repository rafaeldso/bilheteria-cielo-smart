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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

/**
 * From this font scale on, two-line buttons would take about half of a small screen, so the actions stop being a fixed
 * bar and scroll with the content (UI Spec 001 0.4.4 and 002 0.1.1 §4.6).
 */
private const val INLINE_FONT_SCALE = 1.5f

/** True when the actions must scroll with the content instead of staying in a bottom bar. */
@Composable
internal fun actionsScrollWithContent(): Boolean = LocalDensity.current.fontScale >= INLINE_FONT_SCALE

/**
 * Bottom bar of actions (`.sticky-actions` of the kit): `surface` with a divider on top, inside the navigation-bar
 * inset. A local composition of the feature, not a design system component (UI Spec 002 §5).
 */
@Composable
internal fun ActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.navigationBarsPadding().padding(BilheteriaTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.sm),
                content = content,
            )
        }
    }
}
