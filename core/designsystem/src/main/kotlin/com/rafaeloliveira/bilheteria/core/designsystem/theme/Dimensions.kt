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
package com.rafaeloliveira.bilheteria.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale (4dp grid) from `spacing.*` in docs/design/tokens/base.tokens.json.
 * Read it through [BilheteriaTheme.spacing]; layout paddings and gaps come only from here.
 */
@Immutable
data class Spacing(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val xxxl: Dp,
)

/**
 * Fixed sizes from `size.*` in docs/design/tokens/base.tokens.json.
 * Read it through [BilheteriaTheme.sizes].
 */
@Immutable
data class Sizes(
    val touchTargetMin: Dp,
    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val contentMaxWidth: Dp,
)

internal fun bilheteriaSpacing(): Spacing =
    Spacing(
        none = 0.dp,
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = 12.dp,
        lg = 16.dp,
        xl = 24.dp,
        xxl = 32.dp,
        xxxl = 48.dp,
    )

internal fun bilheteriaSizes(): Sizes =
    Sizes(
        touchTargetMin = 48.dp,
        iconSmall = 18.dp,
        iconMedium = 24.dp,
        iconLarge = 40.dp,
        contentMaxWidth = 640.dp,
    )

internal val LocalSpacing = staticCompositionLocalOf { bilheteriaSpacing() }

internal val LocalSizes = staticCompositionLocalOf { bilheteriaSizes() }
