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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import kotlin.io.encoding.Base64

class CieloRequestEncoderTest {
    private val encoder = CieloRequestEncoder()

    @Test
    fun `AC-07 - dado compra confirmada quando pagar entao uri lio payment com callback order response`() {
        val uri = encoder.encodePayment(aPurchase(), TEST_CLIENT_ID, TEST_ACCESS_TOKEN)

        assertTrue(uri.startsWith("lio://payment?request="))
        assertTrue(uri.endsWith("&urlCallback=order://response"))
        assertEquals(1, uri.split("&urlCallback=").size - 1)
    }

    @Test
    fun `AC-07 - dado 2 ingressos de 5000 quando pagar entao json com item, sku do evento e total em centavos`() {
        val purchase = aPurchase(reference = "0b8f2d1c-1111-4222-8333-444455556666", event = anEvent(id = "evt-001"))

        val request = requestOf(encoder.encodePayment(purchase, TEST_CLIENT_ID, TEST_ACCESS_TOKEN))

        val expected =
            buildJsonObject {
                put("accessToken", TEST_ACCESS_TOKEN)
                put("clientID", TEST_CLIENT_ID)
                put("reference", "0b8f2d1c-1111-4222-8333-444455556666")
                put(
                    "items",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("name", purchase.eventName)
                                put("quantity", 2)
                                put("sku", "evt-001")
                                put("unitOfMeasure", "unidade")
                                put("unitPrice", 5_000)
                            },
                        )
                    },
                )
                put("value", "10000")
            }
        assertEquals(expected, request)
    }

    @Test
    fun `AC-07 - dado pagamento sem meio fixado quando pagar entao json nao envia paymentCode`() {
        val request = requestOf(encoder.encodePayment(aPurchase(), TEST_CLIENT_ID, TEST_ACCESS_TOKEN))

        assertFalse("paymentCode" in request)
        assertEquals(JsonPrimitive("10000"), request["value"])
    }

    @Test
    fun `AC-07 - dado nome com acento e caracteres especiais quando pagar entao query sem base64 cru e json intacto`() {
        val name = "Jazz \"Ao Vivo\" ?????????? >>>>>>>>>> à noite"
        val purchase = aPurchase(event = anEvent(name = name))

        val uri = encoder.encodePayment(purchase, TEST_CLIENT_ID, TEST_ACCESS_TOKEN)

        val query = queryOf(uri)
        assertTrue("%2B" in query && "%2F" in query, "premissa do teste: o Base64 deste nome tem + e /")
        assertTrue(query.none { it == '+' || it == '/' || it == '=' })
        val item =
            requestOf(uri)
                .getValue("items")
                .let { it as JsonArray }
                .first()
                .jsonObject
        assertEquals(JsonPrimitive(name), item["name"])
    }

    @Test
    fun `AC-07 - dado paddings diferentes quando pagar entao o sinal igual vai percent encoded`() {
        val queries =
            listOf("a", "aa", "aaa").map { suffix ->
                val purchase = aPurchase(event = anEvent(name = "Show $suffix"))
                queryOf(encoder.encodePayment(purchase, TEST_CLIENT_ID, TEST_ACCESS_TOKEN))
            }

        assertTrue(queries.any { "%3D" in it }, "premissa do teste: algum payload precisa de padding")
        assertTrue(queries.none { '=' in it })
    }

    @Test
    fun `AC-15 - dado recuperacao quando consultar pagina 2 entao uri lio orders com pageSize 5 e page 2`() {
        val uri = encoder.encodeLookup(page = 2, clientId = TEST_CLIENT_ID, accessToken = TEST_ACCESS_TOKEN)

        assertTrue(uri.startsWith("lio://orders?request="))
        assertTrue(uri.endsWith("&urlCallback=order://response"))
        val expected =
            buildJsonObject {
                put("pageSize", 5)
                put("page", 2)
                put("clientID", TEST_CLIENT_ID)
                put("accessToken", TEST_ACCESS_TOKEN)
            }
        assertEquals(expected, requestOf(uri))
    }

    private fun queryOf(uri: String): String = uri.substringAfter("request=").substringBefore("&urlCallback=")

    /** Decodes the `request` parameter the way the Cielo app does: query decoding, then Base64, then JSON. */
    private fun requestOf(uri: String): JsonObject {
        val base64 = URLDecoder.decode(queryOf(uri), Charsets.UTF_8)
        return Json.parseToJsonElement(Base64.Default.decode(base64).decodeToString()).jsonObject
    }
}
