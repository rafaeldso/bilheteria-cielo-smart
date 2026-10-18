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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.SaleResult
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.SpokenDateTime
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.SpokenMoney
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset

class ReceiptRowsTest {
    private val zone = ZoneOffset.ofHours(-3)

    private val transaction = ReceiptTransaction(brand = "Visa", authCode = "A1B2C3", nsu = "000123456")

    private fun aReceipt(transaction: ReceiptTransaction? = null) =
        ReceiptData(
            purchaseNumber = "7F3K-9Q2L",
            eventName = "Noite de Jazz no Parque",
            eventStartsAt = Instant.parse("2026-11-15T23:00:00Z"),
            venue = "Concha Acústica Municipal",
            quantity = 2,
            unitPrice = Money(5_000),
            total = Money(10_000),
            purchasedAt = Instant.parse("2026-10-17T17:32:00Z"),
            transaction = transaction,
        )

    private val saleFields =
        listOf(
            ReceiptField.PurchaseNumber,
            ReceiptField.Event,
            ReceiptField.EventDate,
            ReceiptField.Venue,
            ReceiptField.Quantity,
            ReceiptField.UnitPrice,
            ReceiptField.Total,
            ReceiptField.PurchasedAt,
            ReceiptField.Status,
        )

    @Test
    fun `AC-16 - dado compra aprovada quando exibir comprovante entao mostra campos sem cartao`() {
        val rows = aReceipt(transaction).toRows(SaleResult.Approved, zone)

        assertEquals(
            saleFields + listOf(ReceiptField.Brand, ReceiptField.AuthCode, ReceiptField.Nsu),
            rows.map { it.field },
        )
        assertEquals(ReceiptValue.Text("Visa"), rows.valueOf(ReceiptField.Brand))
        assertEquals(ReceiptValue.Text("A1B2C3"), rows.valueOf(ReceiptField.AuthCode))
        assertEquals(ReceiptValue.Text("000123456"), rows.valueOf(ReceiptField.Nsu))
    }

    @Test
    fun `AC-16 - dado os campos possiveis quando listar entao so os da BR-16 e nenhum numero do cartao`() {
        assertEquals(
            saleFields + listOf(ReceiptField.Brand, ReceiptField.AuthCode, ReceiptField.Nsu),
            ReceiptField.entries,
        )
    }

    @Test
    fun `AC-16 - dado compra negada quando exibir resumo entao omite bandeira autorizacao e nsu`() {
        val rows = aReceipt().toRows(SaleResult.Declined, zone)

        assertEquals(saleFields, rows.map { it.field })
        assertEquals(ReceiptValue.Result(SaleResult.Declined), rows.valueOf(ReceiptField.Status))
    }

    @Test
    fun `AC-16 - dado compra cancelada quando exibir resumo entao omite bandeira autorizacao e nsu`() {
        val rows = aReceipt().toRows(SaleResult.Cancelled, zone)

        assertEquals(saleFields, rows.map { it.field })
        assertEquals(ReceiptValue.Result(SaleResult.Cancelled), rows.valueOf(ReceiptField.Status))
    }

    @Test
    fun `AC-16 - dado dados de transacao em compra negada quando listar campos entao a transacao nunca aparece`() {
        val declinedRows = aReceipt(transaction).toRows(SaleResult.Declined, zone)
        val cancelledRows = aReceipt(transaction).toRows(SaleResult.Cancelled, zone)

        assertEquals(saleFields, declinedRows.map { it.field })
        assertEquals(saleFields, cancelledRows.map { it.field })
    }

    @Test
    fun `AC-16 - dado compra aprovada sem bandeira quando exibir comprovante entao mostra so autorizacao e nsu`() {
        val rows = aReceipt(transaction.copy(brand = null)).toRows(SaleResult.Approved, zone)

        assertEquals(saleFields + listOf(ReceiptField.AuthCode, ReceiptField.Nsu), rows.map { it.field })
    }

    @Test
    fun `AC-16 - dado compra de dois ingressos quando exibir comprovante entao mostra valores em reais e quantidade`() {
        val rows = aReceipt().toRows(SaleResult.Approved, zone)

        assertEquals(ReceiptValue.Tickets(2), rows.valueOf(ReceiptField.Quantity))
        assertEquals(
            ReceiptValue.Amount(formatted = "R$\u00A050,00", spoken = SpokenMoney(reais = 50, centavos = 0)),
            rows.valueOf(ReceiptField.UnitPrice),
        )
        assertEquals(
            ReceiptValue.Amount(formatted = "R$\u00A0100,00", spoken = SpokenMoney(reais = 100, centavos = 0)),
            rows.valueOf(ReceiptField.Total),
        )
    }

    @Test
    fun `dado comprovante reaberto dias depois quando listar campos entao as datas sao absolutas no fuso informado`() {
        val rows = aReceipt().toRows(SaleResult.Approved, zone)

        assertEquals(
            ReceiptValue.Moment(
                dateTime = ReceiptDateTime(date = "15/11/2026", time = "20:00"),
                spoken = SpokenDateTime(day = 15, monthName = "novembro", time = "20:00"),
            ),
            rows.valueOf(ReceiptField.EventDate),
        )
        assertEquals(
            ReceiptValue.Moment(
                dateTime = ReceiptDateTime(date = "17/10/2026", time = "14:32"),
                spoken = SpokenDateTime(day = 17, monthName = "outubro", time = "14:32"),
            ),
            rows.valueOf(ReceiptField.PurchasedAt),
        )
    }

    private fun List<ReceiptRow>.valueOf(field: ReceiptField): ReceiptValue = first { it.field == field }.value
}
