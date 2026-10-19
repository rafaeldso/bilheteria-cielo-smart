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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DsQrCodeTest {
    @Test
    fun `dado matriz de 25 modulos em 600 px quando calcular entao o modulo e inteiro e centralizado`() {
        val layout = qrLayout(side = 25, sizePx = 600)

        // 25 modulos + margem de 4 de cada lado = 33 modulos; 600 / 33 = 18 px por modulo.
        assertEquals(18, layout.module)
        assertEquals((600 - 18 * 25) / 2, layout.origin)
        assertTrue(layout.origin >= 4 * layout.module, "A margem nao pode ser menor que 4 modulos")
    }

    @Test
    fun `dado quadrado menor que a matriz quando calcular entao o modulo tem no minimo 1 px`() {
        assertEquals(1, qrLayout(side = 25, sizePx = 10).module)
    }

    @Test
    fun `dado matriz com tamanho errado quando criar entao falha`() {
        assertThrows(IllegalArgumentException::class.java) { DsQrMatrix(side = 3, modules = BooleanArray(5)) }
    }

    @Test
    fun `dado modulos quando consultar entao devolve o modulo da linha e coluna pedidas`() {
        val matrix = DsQrMatrix(side = 2, modules = booleanArrayOf(true, false, true, true))

        assertTrue(matrix.isDark(0, 0))
        assertEquals(false, matrix.isDark(1, 0)) // x = 1, y = 0: second module of the first row
        assertTrue(matrix.isDark(0, 1)) // x = 0, y = 1: first module of the second row
    }
}
