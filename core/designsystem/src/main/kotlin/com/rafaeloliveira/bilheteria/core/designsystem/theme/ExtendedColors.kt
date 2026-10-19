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
import androidx.compose.ui.graphics.Color

/**
 * Roles that Material 3 does not have (`success`, `warning`, and the fixed QR Code colors), from the tokens.
 * Read them through [BilheteriaTheme.extendedColors]. Always pair the color with text or an icon.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    /** QR Code modules: the same in both themes on purpose, readers need dark on light (`color.qr.*`). */
    val qrForeground: Color,
    val qrBackground: Color,
)

internal fun lightExtendedColors(): ExtendedColors =
    ExtendedColors(
        success = Color(0xFF2E6B30),
        onSuccess = Color(0xFFFFFFFF),
        successContainer = Color(0xFFB1F1A9),
        onSuccessContainer = Color(0xFF002204),
        warning = Color(0xFF7A5900),
        onWarning = Color(0xFFFFFFFF),
        warningContainer = Color(0xFFFFDEA3),
        onWarningContainer = Color(0xFF261900),
        qrForeground = QR_FOREGROUND,
        qrBackground = QR_BACKGROUND,
    )

internal fun darkExtendedColors(): ExtendedColors =
    ExtendedColors(
        success = Color(0xFF96D78F),
        onSuccess = Color(0xFF00390A),
        successContainer = Color(0xFF13521B),
        onSuccessContainer = Color(0xFFB1F1A9),
        warning = Color(0xFFF5BF48),
        onWarning = Color(0xFF402D00),
        warningContainer = Color(0xFF5C4300),
        onWarningContainer = Color(0xFFFFDEA3),
        qrForeground = QR_FOREGROUND,
        qrBackground = QR_BACKGROUND,
    )

private val QR_FOREGROUND = Color(0xFF000000)
private val QR_BACKGROUND = Color(0xFFFFFFFF)

internal val LocalExtendedColors = staticCompositionLocalOf { lightExtendedColors() }
