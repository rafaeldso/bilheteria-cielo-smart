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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * App theme: direction A "Ciano Smart" (docs/design/DESIGN.md). Colors, type and shapes come from
 * the tokens in docs/design/tokens. Dynamic color stays off to keep the identity.
 */
@Composable
fun BilheteriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        remember(darkTheme) { if (darkTheme) darkBilheteriaColorScheme() else lightBilheteriaColorScheme() }
    val extendedColors = remember(darkTheme) { if (darkTheme) darkExtendedColors() else lightExtendedColors() }
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalSpacing provides remember { bilheteriaSpacing() },
        LocalSizes provides remember { bilheteriaSizes() },
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BilheteriaTypography,
            shapes = BilheteriaShapes,
            content = content,
        )
    }
}
