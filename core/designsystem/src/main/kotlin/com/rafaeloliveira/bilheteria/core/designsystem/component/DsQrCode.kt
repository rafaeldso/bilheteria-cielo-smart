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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import kotlin.math.max

/**
 * Square matrix of a QR Code, in plain Kotlin so this module does not depend on the encoding library: the feature
 * converts the library output to it. `true` is a dark module.
 */
class DsQrMatrix(
    val side: Int,
    private val modules: BooleanArray,
) {
    init {
        require(side > 0) { "QR matrix side must be positive" }
        require(modules.size == side * side) { "QR matrix must have side * side modules" }
    }

    fun isDark(
        x: Int,
        y: Int,
    ): Boolean = modules[y * side + x]
}

/**
 * QR Code of a ticket (UI Spec 002, `.qr` of the kit): dark modules on a light square with a 4-module quiet zone, in
 * **both** themes, because readers need dark on light (tokens `qr.foreground` and `qr.background`).
 *
 * Every module is a whole number of pixels, so no edge is blurred; the code is centered when the size is not a
 * multiple of the module count. It is an image for accessibility: it is read through [contentDescription]
 * (e.g. "QR Code do ingresso 2 de 3, Noite de Jazz"), never through the encoded content, and it does not scale with
 * the font size.
 *
 * @param matrix the modules to draw (without the quiet zone, which this component adds).
 * @param contentDescription already localized.
 * @param modifier applied to the root.
 * @param size side of the square, 200dp by default (the minimum for a printed or on-screen ticket).
 */
@Composable
fun DsQrCode(
    matrix: DsQrMatrix,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_QR_SIZE,
) {
    val colors = BilheteriaTheme.extendedColors
    Canvas(
        modifier =
            modifier
                .size(size)
                .clip(MaterialTheme.shapes.small)
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    role = Role.Image
                },
    ) {
        drawRect(color = colors.qrBackground)
        val layout = qrLayout(matrix.side, this.size.minDimension.toInt())
        for (y in 0 until matrix.side) {
            for (x in 0 until matrix.side) {
                if (matrix.isDark(x, y)) {
                    drawRect(
                        color = colors.qrForeground,
                        topLeft =
                            Offset(
                                (layout.origin + x * layout.module).toFloat(),
                                (
                                    layout.origin +
                                        y * layout.module
                                ).toFloat(),
                            ),
                        size = Size(layout.module.toFloat(), layout.module.toFloat()),
                    )
                }
            }
        }
    }
}

/** Where the modules go: [module] whole pixels per module and [origin] pixels of margin on each side. */
internal data class QrLayout(
    val module: Int,
    val origin: Int,
)

/** Layout of a [side]-module matrix plus the 4-module quiet zone in [sizePx] pixels (module of 1px or more). */
internal fun qrLayout(
    side: Int,
    sizePx: Int,
): QrLayout {
    val totalModules = side + 2 * QUIET_ZONE_MODULES
    val module = max(1, sizePx / totalModules)
    val origin = (sizePx - module * side) / 2
    return QrLayout(module = module, origin = origin)
}

private const val QUIET_ZONE_MODULES = 4
private val DEFAULT_QR_SIZE = 200.dp

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsQrCodePreview() {
    DsPreviewSurface { DsQrCode(matrix = previewMatrix(), contentDescription = "QR Code do ingresso 1 de 3") }
}

/** Fictitious matrix (not a valid QR Code): three finder-like squares and a checker pattern. */
private fun previewMatrix(): DsQrMatrix {
    val side = PREVIEW_SIDE
    val modules =
        BooleanArray(side * side) { index ->
            val x = index % side
            val y = index / side
            val finder =
                (x < FINDER && y < FINDER) || (x >= side - FINDER && y < FINDER) || (x < FINDER && y >= side - FINDER)
            if (finder) x % 6 != 1 && y % 6 != 1 else (x * 7 + y * 13) % 3 == 0
        }
    return DsQrMatrix(side, modules)
}

private const val PREVIEW_SIDE = 25
private const val FINDER = 7
