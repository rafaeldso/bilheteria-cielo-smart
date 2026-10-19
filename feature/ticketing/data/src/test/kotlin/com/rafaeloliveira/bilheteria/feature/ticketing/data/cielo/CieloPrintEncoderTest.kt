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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.encoding.Base64

/** Decodes the `request` of a `lio://print` URI back to its JSON, the way the Cielo app reads it. */
internal fun decodePrintRequest(uri: String): String {
    val request = uri.substringAfter("request=").substringBefore("&")
    val base64 = request.replace("%2B", "+").replace("%2F", "/").replace("%3D", "=")
    return Base64.Default.decode(base64).decodeToString()
}

class CieloPrintEncoderTest {
    private val encoder = CieloPrintEncoder()

    @Test
    fun `AC-38 - dado o caminho da imagem quando codificar entao e um PRINT_IMAGE de lio print`() {
        val uri = encoder.encodeImage("/p/a.png")

        assertTrue(uri.startsWith("lio://print?request="))
        assertTrue(uri.endsWith("&urlCallback=order://response"))
        assertEquals(
            """{"operation":"PRINT_IMAGE","styles":[{"key_attributes_align":0}],"value":["/p/a.png"]}""",
            decodePrintRequest(uri),
        )
    }

    @Test
    fun `AC-38 - dado o pedido de impressao quando codificar entao nao leva credenciais nem dados da transacao`() {
        val json = decodePrintRequest(encoder.encodeImage("/x/a.png")).lowercase()

        listOf("accesstoken", "clientid", "authcode", "nsu", "reference").forEach { assertFalse(json.contains(it), it) }
    }

    @Test
    fun `AC-38 - dado base64 com mais e barra quando codificar entao a query sai com percent-encoding`() {
        val uri = encoder.encodeImage("/storage/emulated/0/Pictures/Bilheteria/???>>>.png")

        val query = uri.substringAfter("request=").substringBefore("&")

        assertFalse(query.contains('+') || query.contains('/') || query.contains('='))
    }
}
