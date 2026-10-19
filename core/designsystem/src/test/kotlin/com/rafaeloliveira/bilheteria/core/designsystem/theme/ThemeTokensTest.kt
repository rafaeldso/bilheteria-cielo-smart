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
import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps the theme identical to the design tokens in docs/design/tokens (direction A "Ciano Smart").
 * Changing a token file without changing the theme (or the other way around) fails here.
 */
class ThemeTokensTest {
    private val colorRoles: Map<String, (ColorScheme) -> Color> =
        mapOf(
            "primary" to { it.primary },
            "onPrimary" to { it.onPrimary },
            "primaryContainer" to { it.primaryContainer },
            "onPrimaryContainer" to { it.onPrimaryContainer },
            "secondary" to { it.secondary },
            "onSecondary" to { it.onSecondary },
            "secondaryContainer" to { it.secondaryContainer },
            "onSecondaryContainer" to { it.onSecondaryContainer },
            "tertiary" to { it.tertiary },
            "onTertiary" to { it.onTertiary },
            "tertiaryContainer" to { it.tertiaryContainer },
            "onTertiaryContainer" to { it.onTertiaryContainer },
            "error" to { it.error },
            "onError" to { it.onError },
            "errorContainer" to { it.errorContainer },
            "onErrorContainer" to { it.onErrorContainer },
            "surface" to { it.surface },
            "onSurface" to { it.onSurface },
            "surfaceVariant" to { it.surfaceVariant },
            "onSurfaceVariant" to { it.onSurfaceVariant },
            "surfaceContainerLowest" to { it.surfaceContainerLowest },
            "surfaceContainerLow" to { it.surfaceContainerLow },
            "surfaceContainer" to { it.surfaceContainer },
            "surfaceContainerHigh" to { it.surfaceContainerHigh },
            "surfaceContainerHighest" to { it.surfaceContainerHighest },
            "outline" to { it.outline },
            "outlineVariant" to { it.outlineVariant },
            "scrim" to { it.scrim },
            "inverseSurface" to { it.inverseSurface },
            "inverseOnSurface" to { it.inverseOnSurface },
            "inversePrimary" to { it.inversePrimary },
        )

    private val extendedRoles: Map<String, (ExtendedColors) -> Color> =
        mapOf(
            "success" to { it.success },
            "onSuccess" to { it.onSuccess },
            "successContainer" to { it.successContainer },
            "onSuccessContainer" to { it.onSuccessContainer },
            "warning" to { it.warning },
            "onWarning" to { it.onWarning },
            "warningContainer" to { it.warningContainer },
            "onWarningContainer" to { it.onWarningContainer },
        )

    private val typographyStyles: Map<String, (Typography) -> TextStyle> =
        mapOf(
            "displaySmall" to { it.displaySmall },
            "headlineMedium" to { it.headlineMedium },
            "headlineSmall" to { it.headlineSmall },
            "titleLarge" to { it.titleLarge },
            "titleMedium" to { it.titleMedium },
            "titleSmall" to { it.titleSmall },
            "bodyLarge" to { it.bodyLarge },
            "bodyMedium" to { it.bodyMedium },
            "bodySmall" to { it.bodySmall },
            "labelLarge" to { it.labelLarge },
            "labelMedium" to { it.labelMedium },
            "labelSmall" to { it.labelSmall },
        )

    @Test
    fun `dado tokens claros quando comparar com o ColorScheme claro entao todo papel e identico`() {
        assertSchemeMatchesTokens(tokens = colorTokens("light"), scheme = lightBilheteriaColorScheme())
    }

    @Test
    fun `dado tokens escuros quando comparar com o ColorScheme escuro entao todo papel e identico`() {
        assertSchemeMatchesTokens(tokens = colorTokens("dark"), scheme = darkBilheteriaColorScheme())
    }

    @Test
    fun `dado tokens claros quando comparar com as cores estendidas entao success e warning sao identicos`() {
        assertExtendedMatchesTokens(tokens = colorTokens("light"), extended = lightExtendedColors())
    }

    @Test
    fun `dado tokens escuros quando comparar com as cores estendidas entao success e warning sao identicos`() {
        assertExtendedMatchesTokens(tokens = colorTokens("dark"), extended = darkExtendedColors())
    }

    @Test
    fun `dado tokens do QR Code quando comparar com as cores estendidas entao sao iguais nos dois temas`() {
        val qr =
            loadTokens("base.tokens.json")
                .getValue("color")
                .jsonObject
                .getValue("qr")
                .jsonObject
        val foreground =
            qr
                .getValue(
                    "foreground",
                ).jsonObject
                .getValue("\$value")
                .jsonObject
                .getValue("hex")
                .jsonPrimitive.contentOrNull
                .orEmpty()
        val background =
            qr
                .getValue(
                    "background",
                ).jsonObject
                .getValue("\$value")
                .jsonObject
                .getValue("hex")
                .jsonPrimitive.contentOrNull
                .orEmpty()

        listOf(lightExtendedColors(), darkExtendedColors()).forEach { extended ->
            assertEquals(Color(("FF" + foreground.removePrefix("#")).toLong(HEX_RADIX)), extended.qrForeground)
            assertEquals(Color(("FF" + background.removePrefix("#")).toLong(HEX_RADIX)), extended.qrBackground)
        }
    }

    @Test
    fun `dado tokens de tipografia quando comparar com a Typography entao tamanho peso altura e espacamento batem`() {
        val tokens = loadTokens("base.tokens.json").getValue("typography").jsonObject

        typographyStyles.forEach { (name, style) ->
            val value =
                tokens
                    .getValue(name)
                    .jsonObject
                    .getValue("\$value")
                    .jsonObject
            val size =
                value
                    .getValue("fontSize")
                    .jsonObject
                    .getValue("value")
                    .jsonPrimitive.double
            val ratio = value.getValue("lineHeight").jsonPrimitive.double
            val letterSpacing =
                value
                    .getValue("letterSpacing")
                    .jsonObject
                    .getValue("value")
                    .jsonPrimitive.double
            val weight =
                value
                    .getValue("fontWeight")
                    .jsonPrimitive.double
                    .toInt()
            val actual = style(BilheteriaTypography)

            assertEquals(size, actual.fontSize.value.toDouble(), FLOAT_DELTA, "$name fontSize")
            assertEquals(size * ratio, actual.lineHeight.value.toDouble(), LINE_HEIGHT_ROUNDING, "$name lineHeight")
            assertEquals(letterSpacing, actual.letterSpacing.value.toDouble(), FLOAT_DELTA, "$name letterSpacing")
            assertEquals(FontWeight(weight), actual.fontWeight, "$name fontWeight")
        }
    }

    @Test
    fun `dado tokens de forma quando comparar com Shapes entao os raios batem`() {
        val tokens = loadTokens("base.tokens.json").getValue("shape").jsonObject
        val actual =
            mapOf(
                "extraSmall" to BilheteriaShapes.extraSmall,
                "small" to BilheteriaShapes.small,
                "medium" to BilheteriaShapes.medium,
                "large" to BilheteriaShapes.large,
                "extraLarge" to BilheteriaShapes.extraLarge,
            )

        actual.forEach { (name, shape) ->
            val radius =
                tokens
                    .getValue(name)
                    .jsonObject
                    .getValue("\$value")
                    .jsonObject
                    .getValue("value")
            val outline = shape.createOutline(Size(SHAPE_BOX, SHAPE_BOX), LayoutDirection.Ltr, Density(1f))
            val corner = (outline as Outline.Rounded).roundRect.topLeftCornerRadius.x
            assertEquals(radius.jsonPrimitive.double.toFloat(), corner, 0f, "$name radius")
        }
    }

    @Test
    fun `dado tokens de espacamento quando comparar com Spacing entao todos os valores batem`() {
        val spacing = bilheteriaSpacing()
        val actual =
            mapOf(
                "none" to spacing.none,
                "xxs" to spacing.xxs,
                "xs" to spacing.xs,
                "sm" to spacing.sm,
                "md" to spacing.md,
                "lg" to spacing.lg,
                "xl" to spacing.xl,
                "xxl" to spacing.xxl,
                "xxxl" to spacing.xxxl,
            )

        assertDimensionsMatchTokens(group = "spacing", actual = actual)
    }

    @Test
    fun `dado tokens de tamanho quando comparar com Sizes entao todos os valores batem`() {
        val sizes = bilheteriaSizes()
        val actual =
            mapOf(
                "touchTargetMin" to sizes.touchTargetMin,
                "iconSmall" to sizes.iconSmall,
                "iconMedium" to sizes.iconMedium,
                "iconLarge" to sizes.iconLarge,
                "contentMaxWidth" to sizes.contentMaxWidth,
            )

        assertDimensionsMatchTokens(group = "size", actual = actual)
    }

    private fun assertDimensionsMatchTokens(
        group: String,
        actual: Map<String, Dp>,
    ) {
        val tokens =
            loadTokens("base.tokens.json")
                .getValue(group)
                .jsonObject
                .filterKeys { !it.startsWith("$") }
        assertEquals(tokens.keys, actual.keys, "$group tokens without a theme value, or the other way around")
        actual.forEach { (name, dp) ->
            val value =
                tokens
                    .getValue(name)
                    .jsonObject
                    .getValue("\$value")
                    .jsonObject
                    .getValue("value")
                    .jsonPrimitive.double
            assertEquals(value, dp.value.toDouble(), FLOAT_DELTA, "$group.$name")
        }
    }

    private fun assertSchemeMatchesTokens(
        tokens: Map<String, Color>,
        scheme: ColorScheme,
    ) {
        val unmapped = tokens.keys - colorRoles.keys - extendedRoles.keys
        assertTrue(unmapped.isEmpty(), "Token roles without a ColorScheme mapping: $unmapped")
        colorRoles.forEach { (name, role) ->
            assertEquals(tokens.getValue(name), role(scheme), "ColorScheme.$name")
        }
    }

    private fun assertExtendedMatchesTokens(
        tokens: Map<String, Color>,
        extended: ExtendedColors,
    ) {
        extendedRoles.forEach { (name, role) ->
            assertEquals(tokens.getValue(name), role(extended), "ExtendedColors.$name")
        }
    }

    private fun colorTokens(mode: String): Map<String, Color> {
        val colors = loadTokens("color.$mode.tokens.json").getValue("color").jsonObject
        return colors
            .filterKeys { !it.startsWith("$") }
            .mapValues { (name, node) ->
                val hex =
                    node.jsonObject
                        .getValue("\$value")
                        .jsonObject
                        .getValue("hex")
                        .jsonPrimitive.contentOrNull
                requireNotNull(hex) { "Token $name has no hex" }
                Color(HEX_OPAQUE_ALPHA or hex.removePrefix("#").toLong(HEX_RADIX))
            }
    }

    private fun loadTokens(fileName: String): JsonObject {
        val file = tokensDirectory().resolve(fileName)
        return Json.parseToJsonElement(file.readText()).jsonObject
    }

    private fun tokensDirectory(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = dir.resolve("docs/design/tokens")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        error("docs/design/tokens not found above ${System.getProperty("user.dir")}")
    }

    private companion object {
        const val HEX_RADIX = 16
        const val HEX_OPAQUE_ALPHA = 0xFF000000L
        const val LINE_HEIGHT_ROUNDING = 0.5
        const val FLOAT_DELTA = 0.0001
        const val SHAPE_BOX = 200f
    }
}
