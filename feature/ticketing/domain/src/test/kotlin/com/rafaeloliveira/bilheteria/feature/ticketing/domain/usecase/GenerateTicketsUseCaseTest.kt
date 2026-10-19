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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GenerateTicketsUseCaseTest {
    private val generate = GenerateTicketsUseCase()

    @Test
    fun `AC-29 - dado compra aprovada de 3 quando gerar entao 3 codigos distintos ligados a compra`() {
        val purchase = aPurchase(reference = "ref-1", quantity = 3, status = PurchaseStatus.Approved)

        val set = generate(purchase)

        assertNotNull(set)
        assertEquals(listOf(1, 2, 3), set!!.tickets.map { it.ordinal })
        assertTrue(set.tickets.all { it.total == 3 })
        assertEquals(
            listOf("bilheteria:1:ref-1:1", "bilheteria:1:ref-1:2", "bilheteria:1:ref-1:3"),
            set.tickets.map { it.code.value },
        )
        assertEquals(
            3,
            set.tickets
                .map { it.code }
                .toSet()
                .size,
        )
    }

    @Test
    fun `AC-30 - dado compra com dados da transacao quando gerar entao o codigo nao contem nenhum deles`() {
        val transaction = aTransaction()
        val purchase = aPurchase(quantity = 2, status = PurchaseStatus.Approved, transaction = transaction)

        val content = generate(purchase)!!.tickets.joinToString(" ") { it.code.value }

        listOf(transaction.orderId, transaction.authCode, transaction.nsu, transaction.brand!!, purchase.eventName)
            .forEach { sensitive -> assertFalse(content.contains(sensitive), "O codigo nao pode conter $sensitive") }
    }

    @Test
    fun `AC-30 - dado um codigo quando converter em texto entao o conteudo e mascarado`() {
        val code =
            generate(
                aPurchase(reference = "ref-secret", status = PurchaseStatus.Approved),
            )!!.tickets.first().code

        assertFalse(code.toString().contains("ref-secret"))
    }

    @Test
    fun `AC-31 - dado a mesma compra quando gerar duas vezes entao o conteudo e identico`() {
        val purchase = aPurchase(quantity = 2, status = PurchaseStatus.Approved)

        assertEquals(generate(purchase), generate(purchase))
    }

    @ParameterizedTest
    @EnumSource(PurchaseStatus::class, names = ["Approved"], mode = EnumSource.Mode.EXCLUDE)
    fun `AC-32 - dado compra que nao esta aprovada quando gerar entao nenhum ingresso`(status: PurchaseStatus) {
        assertNull(generate(aPurchase(status = status)))
    }
}
