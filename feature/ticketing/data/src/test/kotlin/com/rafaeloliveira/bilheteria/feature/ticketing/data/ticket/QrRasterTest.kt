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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QrRasterTest {
    @Test
    fun `AC-29 - dado matriz de 25 modulos em 600 px quando calcular entao o modulo e inteiro`() {
        // 25 + 2 * 4 = 33 modulos; 600 / 33 = 18 px.
        assertEquals(18, QrRaster.modulePixels(side = 25, sizePx = 600))
    }

    @Test
    fun `AC-29 - dado quadrado menor que a matriz quando calcular entao o modulo tem 1 px`() {
        assertEquals(1, QrRaster.modulePixels(side = 25, sizePx = 10))
    }

    @Test
    fun `AC-29 - dado uma matriz 1x1 escura quando desenhar entao a margem e clara e o modulo e preto`() {
        val pixels = QrRaster.render(QrMatrix(1, booleanArrayOf(true)), sizePx = 9)

        // 1 + 8 = 9 modulos de 1 px: o modulo escuro fica no centro (4, 4).
        assertEquals(QrRaster.DARK, pixels[4 * 9 + 4])
        assertEquals(QrRaster.LIGHT, pixels[0])
        assertEquals(QrRaster.LIGHT, pixels[9 * 9 - 1])
    }
}
