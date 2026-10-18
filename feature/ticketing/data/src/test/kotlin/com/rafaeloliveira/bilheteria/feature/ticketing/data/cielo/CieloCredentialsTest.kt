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

import com.rafaeloliveira.bilheteria.feature.ticketing.data.BuildConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CieloCredentialsTest {
    @Test
    fun `AC-07 - dado credenciais quando imprimir entao toString nao revela token nem client id`() {
        val text = CieloCredentials(TEST_CLIENT_ID, TEST_ACCESS_TOKEN).toString()

        assertFalse(TEST_CLIENT_ID in text || TEST_ACCESS_TOKEN in text)
    }

    @Test
    fun `AC-07 - dado DTOs do pedido quando imprimir entao toString nao revela token nem client id`() {
        val payment =
            PaymentRequestDto(
                accessToken = TEST_ACCESS_TOKEN,
                clientId = TEST_CLIENT_ID,
                reference = "r",
                items = emptyList(),
                value = "100",
            )
        val lookup =
            LookupRequestDto(pageSize = 5, page = 0, clientId = TEST_CLIENT_ID, accessToken = TEST_ACCESS_TOKEN)

        listOf(payment.toString(), lookup.toString()).forEach {
            assertFalse(TEST_CLIENT_ID in it || TEST_ACCESS_TOKEN in it)
        }
    }

    @Test
    fun `AC-07 - dado build sem propriedades cielo quando ler BuildConfig entao usa valores ficticios`() {
        assertTrue(BuildConfig.CIELO_CLIENT_ID.isNotBlank())
        assertTrue(BuildConfig.CIELO_ACCESS_TOKEN.isNotBlank())
    }
}
