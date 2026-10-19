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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketPrinter
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class PrintTicketsUseCaseTest {
    private val printer = FakeTicketPrinter()
    private val approved = aPurchase(quantity = 3, status = PurchaseStatus.Approved, transaction = aTransaction())

    private fun useCase(vararg purchases: Purchase) =
        FakePurchaseRepository(purchases.toList()).let { repository ->
            PrintTicketsUseCase(LoadTicketSetUseCase(repository, GenerateTicketsUseCase()), repository, printer)
        }

    @Test
    fun `AC-38 - dado compra aprovada de 3 quando imprimir entao envia os 3 ingressos desde o primeiro`() =
        runTest {
            val result = useCase(approved)(approved.id)

            assertEquals(DataResult.Success(Unit), result)
            val (set, from) = printer.requests.single()
            assertEquals(3, set.tickets.size)
            assertEquals(0, from)
            assertEquals(approved.eventName, set.eventName)
        }

    @ParameterizedTest
    @EnumSource(PurchaseStatus::class, names = ["Approved"], mode = EnumSource.Mode.EXCLUDE)
    fun `AC-38 - dado compra que nao esta aprovada quando imprimir entao nada e enviado`(status: PurchaseStatus) =
        runTest {
            val purchase = aPurchase(status = status)

            val result = useCase(purchase)(purchase.id)

            assertTrue(result is DataResult.Failure)
            assertEquals(0, printer.requests.size)
        }

    @Test
    fun `AC-39 - dado impressao em andamento quando imprimir de novo entao nenhuma nova impressao e enviada`() =
        runTest {
            val useCase = useCase(approved)
            useCase(approved.id)

            val again = useCase(approved.id)

            assertEquals(DataResult.Success(Unit), again)
            assertEquals(1, printer.requests.size)
        }

    @Test
    fun `AC-40 - dado sem papel depois do 1o quando tentar de novo entao so os ingressos 2 e 3 sao enviados`() =
        runTest {
            printer.emit(PrintStatus.Failed(approved.id, PrintFailureReason.NoPaper, printed = 1, total = 3))

            useCase(approved)(approved.id)

            assertEquals(1, printer.requests.single().second)
        }

    @Test
    fun `AC-40 - dado falha de outra compra quando imprimir esta entao comeca do primeiro ingresso`() =
        runTest {
            printer.emit(PrintStatus.Failed("outra-compra", PrintFailureReason.PrinterError, printed = 2, total = 3))

            useCase(approved)(approved.id)

            assertEquals(0, printer.requests.single().second)
        }

    @Test
    fun `AC-40 - dado app Cielo ausente quando imprimir entao falha com terminal indisponivel sem alterar a compra`() =
        runTest {
            printer.terminalAvailable = false

            val result = useCase(approved)(approved.id)

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
        }

    @Test
    fun `AC-44 - dado impressao concluida quando imprimir de novo entao recomeca do primeiro ingresso`() =
        runTest {
            printer.emit(PrintStatus.Done(approved.id, total = 3))

            useCase(approved)(approved.id)

            assertEquals(0, printer.requests.single().second)
        }

    @Test
    fun `AC-40 - dado compra em andamento quando imprimir entao nada e enviado para nao confundir as respostas`() =
        runTest {
            val pending = aPurchase(reference = "pending", status = PurchaseStatus.Pending)

            val result = useCase(approved, pending)(approved.id)

            assertEquals(DataResult.Failure(TicketingError.PurchaseAlreadyOpen), result)
            assertEquals(0, printer.requests.size)
        }
}
