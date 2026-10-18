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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

/**
 * Frame of every preview of this module: the app theme on a `surface` background.
 *
 * @param modifier applied to the background, e.g. a fixed width to show a full-width component.
 * @param padded false for components that fill the screen edge to edge (lists, state messages, bars).
 */
@Composable
internal fun DsPreviewSurface(
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    content: @Composable () -> Unit,
) {
    BilheteriaTheme {
        Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = if (padded) Modifier.padding(BilheteriaTheme.spacing.lg) else Modifier) { content() }
        }
    }
}
