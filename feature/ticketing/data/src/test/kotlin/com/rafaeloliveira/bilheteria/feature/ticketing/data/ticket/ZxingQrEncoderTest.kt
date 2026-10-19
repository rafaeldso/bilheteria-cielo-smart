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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZxingQrEncoderTest {
    private val encoder = ZxingQrEncoder()
    private val code = TicketCode("bilheteria:1:91ccb5d9-ccf6-4471-bfe2-413d5bc48637:2")

    @Test
    fun `AC-29 - dado o codigo de um ingresso quando codificar e desenhar entao o ZXing decodifica o mesmo`() {
        val matrix = (encoder.encode(code) as DataResult.Success).value

        for (sizePx in listOf(200, 384, 600)) {
            val pixels = QrRaster.render(matrix, sizePx)

            assertEquals(code.value, decodeQr(pixels, sizePx, sizePx), "QR de $sizePx px")
        }
    }

    @Test
    fun `AC-29 - dado o codigo quando codificar entao a matriz e quadrada e nao tem margem propria`() {
        val matrix = (encoder.encode(code) as DataResult.Success).value

        // Versao 3 (29 modulos) ou maior para 51 caracteres com correcao M; finder pattern no canto: modulo escuro.
        assertTrue(matrix.side >= MIN_SIDE)
        assertTrue(matrix.isDark(0, 0), "O canto superior esquerdo e o marcador de posicao, sem margem")
    }

    @Test
    fun `AC-31 - dado o mesmo codigo quando codificar duas vezes entao a matriz e identica`() {
        val first = (encoder.encode(code) as DataResult.Success).value
        val second = (encoder.encode(code) as DataResult.Success).value

        assertEquals(first, second)
    }

    @Test
    fun `AC-29 - dado codigos de ingressos diferentes da mesma compra quando codificar entao as matrizes diferem`() {
        val first = (encoder.encode(TicketCode("bilheteria:1:ref:1")) as DataResult.Success).value
        val second = (encoder.encode(TicketCode("bilheteria:1:ref:2")) as DataResult.Success).value

        assertTrue(first != second)
    }

    @Test
    fun `AC-35 - dado conteudo grande demais quando codificar entao falha sem expor o conteudo`() {
        val tooBig = TicketCode("x".repeat(OVER_CAPACITY))

        val result = encoder.encode(tooBig)

        assertInstanceOf(DataResult.Failure::class.java, result)
    }

    private companion object {
        const val MIN_SIDE = 21
        const val OVER_CAPACITY = 5_000
    }
}
