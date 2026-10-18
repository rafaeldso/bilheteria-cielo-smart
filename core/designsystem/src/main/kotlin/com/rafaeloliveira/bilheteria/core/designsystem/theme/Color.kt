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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Roles missing from the token files are derived from tokens, never invented:
 * background = surface, surfaceTint = primary, surfaceBright/surfaceDim = surface/surfaceContainer tones.
 */

/**
 * Light roles of direction A "Ciano Smart" (docs/design/tokens/color.light.tokens.json).
 * Values must stay identical to the token file; `ThemeTokensTest` compares them.
 */
internal fun lightBilheteriaColorScheme(): ColorScheme =
    lightColorScheme(
        primary = Color(0xFF00658F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC7E7FF),
        onPrimaryContainer = Color(0xFF001E2E),
        inversePrimary = Color(0xFF85CFFF),
        secondary = Color(0xFF3B5F84),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD2E4FF),
        onSecondaryContainer = Color(0xFF001D36),
        tertiary = Color(0xFF615A7C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE7DEFF),
        onTertiaryContainer = Color(0xFF1D1736),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF6FAFE),
        onBackground = Color(0xFF171C20),
        surface = Color(0xFFF6FAFE),
        onSurface = Color(0xFF171C20),
        surfaceVariant = Color(0xFFDDE3EA),
        onSurfaceVariant = Color(0xFF41484D),
        surfaceTint = Color(0xFF00658F),
        inverseSurface = Color(0xFF2C3135),
        inverseOnSurface = Color(0xFFEDF1F5),
        outline = Color(0xFF71787E),
        outlineVariant = Color(0xFFC1C7CE),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF6FAFE),
        surfaceDim = Color(0xFFE4E9ED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF0F4F8),
        surfaceContainer = Color(0xFFEAEEF2),
        surfaceContainerHigh = Color(0xFFE4E9ED),
        surfaceContainerHighest = Color(0xFFDFE3E7),
    )

/**
 * Dark roles of direction A "Ciano Smart" (docs/design/tokens/color.dark.tokens.json).
 * Values must stay identical to the token file; `ThemeTokensTest` compares them.
 */
internal fun darkBilheteriaColorScheme(): ColorScheme =
    darkColorScheme(
        primary = Color(0xFF85CFFF),
        onPrimary = Color(0xFF00344C),
        primaryContainer = Color(0xFF004C6C),
        onPrimaryContainer = Color(0xFFC7E7FF),
        inversePrimary = Color(0xFF00658F),
        secondary = Color(0xFFA2C9F7),
        onSecondary = Color(0xFF003258),
        secondaryContainer = Color(0xFF224A70),
        onSecondaryContainer = Color(0xFFD2E4FF),
        tertiary = Color(0xFFCBC1E9),
        onTertiary = Color(0xFF332C4C),
        tertiaryContainer = Color(0xFF4A4263),
        onTertiaryContainer = Color(0xFFE7DEFF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0F1418),
        onBackground = Color(0xFFDFE3E7),
        surface = Color(0xFF0F1418),
        onSurface = Color(0xFFDFE3E7),
        surfaceVariant = Color(0xFF41484D),
        onSurfaceVariant = Color(0xFFC1C7CE),
        surfaceTint = Color(0xFF85CFFF),
        inverseSurface = Color(0xFFDFE3E7),
        inverseOnSurface = Color(0xFF2C3135),
        outline = Color(0xFF8B9198),
        outlineVariant = Color(0xFF41484D),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF31363A),
        surfaceDim = Color(0xFF0F1418),
        surfaceContainerLowest = Color(0xFF0A0F12),
        surfaceContainerLow = Color(0xFF171C20),
        surfaceContainer = Color(0xFF1B2024),
        surfaceContainerHigh = Color(0xFF262B2F),
        surfaceContainerHighest = Color(0xFF31363A),
    )
