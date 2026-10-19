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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.ticket

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import kotlin.math.max

/**
 * Pixels of a QR Code, with the same rules as `DsQrCode` so the screen, the printed ticket and the shared image
 * agree (plan §4): dark on light, a 4-module quiet zone, and a **whole** number of pixels per module so no edge is
 * blurred (a fractional module can make a reader fail). Pure Kotlin: testable on the JVM.
 */
internal object QrRaster {
    const val DARK: Int = 0xFF000000.toInt()
    const val LIGHT: Int = 0xFFFFFFFF.toInt()
    private const val QUIET_ZONE_MODULES = 4

    /** Pixels per module for a QR Code of [side] modules drawn in a square of [sizePx] pixels (1 or more). */
    fun modulePixels(
        side: Int,
        sizePx: Int,
    ): Int = max(1, sizePx / (side + 2 * QUIET_ZONE_MODULES))

    /** ARGB pixels of the square image ([sizePx] x [sizePx], row by row), centered with the quiet zone around it. */
    fun render(
        matrix: QrMatrix,
        sizePx: Int,
    ): IntArray {
        val module = modulePixels(matrix.side, sizePx)
        val origin = (sizePx - module * matrix.side) / 2
        val pixels = IntArray(sizePx * sizePx) { LIGHT }
        for (y in 0 until matrix.side) {
            for (x in 0 until matrix.side) {
                if (matrix.isDark(x, y)) fillModule(pixels, sizePx, origin + x * module, origin + y * module, module)
            }
        }
        return pixels
    }

    private fun fillModule(
        pixels: IntArray,
        sizePx: Int,
        left: Int,
        top: Int,
        module: Int,
    ) {
        for (dy in 0 until module) {
            pixels.fill(DARK, (top + dy) * sizePx + left, (top + dy) * sizePx + left + module)
        }
    }
}
