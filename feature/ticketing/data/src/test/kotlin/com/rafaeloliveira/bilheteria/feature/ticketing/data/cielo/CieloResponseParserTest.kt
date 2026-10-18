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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.LookupOrder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.io.encoding.Base64

class CieloResponseParserTest {
    private val parser = CieloResponseParser()

    private val ourReference = PurchaseReference("0b8f2d1c-1111-4222-8333-444455556666")

    private fun parseFixture(name: String): PaymentOutcome = parser.parse(cieloBase64(fixture(name)))

    @Test
    fun `AC-09 - dado retorno autorizado da doc quando processar entao Approved com valor e dados da transacao`() {
        val outcome = parseFixture("payment_approved.json")

        val expected =
            PaymentOutcome.Approved(
                reference = ourReference,
                amount = Money(10_000),
                transaction =
                    TransactionData(
                        orderId = "6b1f7a2e-0000-4000-8000-00000000a001",
                        authCode = "123456",
                        nsu = "987654",
                        brand = "mock_brand",
                    ),
            )
        assertEquals(expected, outcome)
    }

    @Test
    fun `AC-09 - dado paidAmount zero e payments amount 10000 quando processar entao valor vem de payments amount`() {
        val outcome = parseFixture("payment_approved.json")

        assertEquals(Money(10_000), (outcome as PaymentOutcome.Approved).amount)
    }

    @Test
    fun `AC-09 - dado brand vazia e statusCode numerico quando processar entao Approved com bandeira nula`() {
        val outcome = parseFixture("payment_approved_empty_brand.json")

        assertEquals(null, (outcome as PaymentOutcome.Approved).transaction.brand)
    }

    @Test
    fun `AC-09 - dado pagamento Pix statusCode 0 quando processar entao Approved`() {
        val outcome = parseFixture("payment_approved_pix.json")

        assertTrue(outcome is PaymentOutcome.Approved)
    }

    @Test
    fun `AC-09 - dado retorno com bin mask pan e document quando processar entao nada sensivel chega ao modelo`() {
        val raw = fixture("payment_approved.json")
        val sensitive =
            listOf("411111", "11222333000181", "FULANO", "fake-access-key", "FAKECRYPTOGRAM", "FAKESIGNATURE")
        assertTrue(sensitive.all { it in raw }, "premissa do teste: a fixture traz os campos sensiveis")

        val shown = parser.parse(cieloBase64(raw)).toString()

        assertTrue(sensitive.none { it in shown }, "modelo nao pode conter dado sensivel: $shown")
    }

    @Test
    fun `AC-09 - dado status ENTERED com pagamento autorizado quando processar entao classifica pelo pagamento`() {
        val outcome = parseFixture("payment_entered.json")

        assertTrue(outcome is PaymentOutcome.Approved)
    }

    @Test
    fun `AC-10 - dado codigo 1 quando processar entao Cancelled`() {
        assertEquals(PaymentOutcome.Cancelled(reference = null), parseFixture("error_code_1.json"))
    }

    @Test
    fun `AC-11 - dado codigo 3 quando processar entao Declined`() {
        assertEquals(PaymentOutcome.Declined(reference = null), parseFixture("error_code_3.json"))
    }

    @Test
    fun `AC-13 - dado codigo 2 quando processar entao Failed generico`() {
        val expected = PaymentOutcome.Failed(reference = null, reason = FailureReason.Generic)

        assertEquals(expected, parseFixture("error_code_2.json"))
    }

    @Test
    fun `AC-13 - dado codigo 4 quando processar entao Failed de autenticacao`() {
        val expected = PaymentOutcome.Failed(reference = null, reason = FailureReason.Authentication)

        assertEquals(expected, parseFixture("error_code_4.json"))
    }

    @Test
    fun `AC-13 - dado codigo desconhecido quando processar entao Failed generico`() {
        val expected = PaymentOutcome.Failed(reference = null, reason = FailureReason.Generic)

        assertEquals(expected, parseFixture("error_code_unknown.json"))
    }

    @Test
    fun `AC-13 - dado codigo como texto numerico quando processar entao mapeia como o inteiro`() {
        val outcome = parser.parse(cieloBase64("""{"code":"1","reason":"CANCELADO PELO USUARIO"}"""))

        assertEquals(PaymentOutcome.Cancelled(reference = null), outcome)
    }

    @Test
    fun `AC-14 - dado resposta ausente quando processar entao Unreadable`() {
        assertEquals(PaymentOutcome.Unreadable, parser.parse(null))
        assertEquals(PaymentOutcome.Unreadable, parser.parse(""))
    }

    @Test
    fun `AC-14 - dado texto que nao e Base64 quando processar entao Unreadable`() {
        assertEquals(PaymentOutcome.Unreadable, parser.parse("isto nao e base64 !!!"))
    }

    @Test
    fun `AC-14 - dado Base64 de texto que nao e JSON quando processar entao Unreadable`() {
        assertEquals(PaymentOutcome.Unreadable, parser.parse(cieloBase64("not json")))
    }

    @Test
    fun `AC-14 - dado JSON truncado quando processar entao Unreadable`() {
        val truncated = fixture("payment_approved.json").take(TRUNCATED_LENGTH)

        assertEquals(PaymentOutcome.Unreadable, parser.parse(cieloBase64(truncated)))
    }

    @ParameterizedTest
    @MethodSource("unreadableResponses")
    fun `AC-14 - dado campos ausentes ou invalidos quando processar entao Unreadable e nunca Approved`(json: String) {
        assertEquals(PaymentOutcome.Unreadable, parser.parse(cieloBase64(json)))
    }

    @Test
    fun `AC-14 - dado Base64 com mais e virado em espaco pelo query decoding quando processar entao le a resposta`() {
        val json = """{"code":1,"reason":"CANCELADO >>>>>>>>>>>>"}"""
        val base64 = cieloBase64(json)
        assertTrue('+' in base64, "premissa do teste: o Base64 tem +")

        val outcome = parser.parse(base64.replace('+', ' '))

        assertEquals(PaymentOutcome.Cancelled(reference = null), outcome)
    }

    @Test
    fun `AC-14 - dado resposta com exatamente o tamanho maximo quando processar entao e lida`() {
        val outcome = parser.parse(base64WithLength(CieloResponseParser.MAX_RESPONSE_LENGTH))

        assertEquals(PaymentOutcome.Cancelled(reference = null), outcome)
    }

    @Test
    fun `AC-14 - dado resposta acima do tamanho maximo quando processar entao Unreadable`() {
        val outcome = parser.parse(base64WithLength(CieloResponseParser.MAX_RESPONSE_LENGTH + BASE64_GROUP))

        assertEquals(PaymentOutcome.Unreadable, outcome)
    }

    @Test
    fun `AC-15 - dado listagem da doc quando processar entao pagina normalizada e so pedidos com reference`() {
        val outcome = parseFixture("orders_page.json")

        val expected =
            PaymentOutcome.LookupPage(
                page = 2,
                totalPages = 3,
                orders =
                    listOf(
                        LookupOrder(
                            reference = ourReference,
                            approved =
                                PaymentOutcome.Approved(
                                    reference = ourReference,
                                    amount = Money(10_000),
                                    transaction =
                                        TransactionData(
                                            orderId = "6b1f7a2e-0000-4000-8000-00000000b001",
                                            authCode = "123456",
                                            nsu = "987654",
                                            brand = "mock_brand",
                                        ),
                                ),
                        ),
                        LookupOrder(PurchaseReference("aaaa2d1c-1111-4222-8333-444455556666"), approved = null),
                        LookupOrder(PurchaseReference("bbbb2d1c-1111-4222-8333-444455556666"), approved = null),
                    ),
            )
        assertEquals(expected, outcome)
    }

    @Test
    fun `AC-15 - dado listagem com status ENTERED e pagamento autorizado quando processar entao pedido aprovado`() {
        val page = parseFixture("orders_page.json") as PaymentOutcome.LookupPage

        assertTrue(page.orders.first { it.reference == ourReference }.approved != null)
    }

    @Test
    fun `AC-15 - dado listagem com bin mask e document quando processar entao nada sensivel chega ao modelo`() {
        val shown = parseFixture("orders_page.json").toString()

        assertFalse("411111" in shown || "11222333000181" in shown)
    }

    @Test
    fun `AC-15 - dado currentPage 1 da Cielo quando processar entao page do dominio e 0`() {
        val first = """{"currentPage":1,"totalPages":4,"results":[]}"""

        val outcome = parser.parse(cieloBase64(first))

        assertEquals(PaymentOutcome.LookupPage(page = 0, totalPages = 4, orders = emptyList()), outcome)
    }

    @Test
    fun `AC-15 - dado listagem vazia quando processar entao pagina sem pedidos`() {
        val empty = """{"currentPage":0,"totalPages":0,"results":[]}"""

        val outcome = parser.parse(cieloBase64(empty))

        assertEquals(PaymentOutcome.LookupPage(page = 0, totalPages = 0, orders = emptyList()), outcome)
    }

    @Test
    fun `AC-15 - dado pedido pago incompleto quando processar entao Unreadable e nunca nao encontrado`() {
        assertEquals(PaymentOutcome.Unreadable, parseFixture("orders_page_paid_incomplete.json"))
    }

    @Test
    fun `AC-15 - dado listagem sem paginacao quando processar entao Unreadable`() {
        assertEquals(PaymentOutcome.Unreadable, parser.parse(cieloBase64("""{"results":[]}""")))
    }

    /** A valid Cielo error response whose Base64 has exactly [length] characters (no padding, no line breaks). */
    private fun base64WithLength(length: Int): String {
        val bytes = length / BASE64_GROUP * BYTES_PER_GROUP
        val prefix = """{"code":1,"notes":""""
        val suffix = """"}"""
        val json = prefix + "x".repeat(bytes - prefix.length - suffix.length) + suffix
        return Base64.Default.encode(json.encodeToByteArray()).also { assertEquals(length, it.length) }
    }

    companion object {
        private const val TRUNCATED_LENGTH = 120
        private const val BASE64_GROUP = 4
        private const val BYTES_PER_GROUP = 3

        /** A payment order; a null argument omits the field. */
        private fun order(
            id: String? = "o1",
            reference: String? = "r1",
            amount: String? = "100",
            authCode: String? = "a",
            cieloCode: String? = "c",
            statusCode: String = "1",
        ): String {
            val payment =
                listOfNotNull(
                    amount?.let { "\"amount\":$it" },
                    authCode?.let { "\"authCode\":\"$it\"" },
                    cieloCode?.let { "\"cieloCode\":\"$it\"" },
                    "\"paymentFields\":{\"statusCode\":\"$statusCode\"}",
                ).joinToString(",", "{", "}")
            val fields =
                listOfNotNull(
                    id?.let { "\"id\":\"$it\"" },
                    reference?.let { "\"reference\":\"$it\"" },
                    "\"payments\":[$payment]",
                )
            return fields.joinToString(",", "{", "}")
        }

        @JvmStatic
        fun unreadableResponses(): List<String> =
            listOf(
                "[]",
                "{}",
                """{"reference":"r1","payments":[]}""",
                order(statusCode = "2"),
                order(id = null),
                order(reference = null),
                order(reference = "  "),
                order(amount = null),
                order(amount = "\"abc\""),
                order(amount = "-5"),
                order(authCode = null),
                order(cieloCode = null),
                """{"code":"abc","reason":"x"}""",
                """{"code":null}""",
                """{"id":{"nested":1},"reference":"r1"}""",
            )
    }
}
