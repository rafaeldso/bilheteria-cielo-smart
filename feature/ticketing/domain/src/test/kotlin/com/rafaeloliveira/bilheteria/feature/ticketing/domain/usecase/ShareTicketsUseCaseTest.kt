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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketFiles
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketPrinter
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShareTicketsUseCaseTest {
    private val files = FakeTicketFiles()
    private val printer = FakeTicketPrinter()
    private val approved = aPurchase(quantity = 2, status = PurchaseStatus.Approved)

    private fun useCase(vararg purchases: Purchase) =
        ShareTicketsUseCase(
            LoadTicketSetUseCase(FakePurchaseRepository(purchases.toList()), GenerateTicketsUseCase()),
            files,
            printer,
        )

    @Test
    fun `AC-41 - dado compra aprovada de 2 quando compartilhar entao prepara uma imagem por ingresso`() =
        runTest {
            val bundle = (useCase(approved)(approved.id) as DataResult.Success).value

            assertEquals(2, bundle?.uris?.size)
            assertEquals(1, files.prepared.size)
        }

    @Test
    fun `AC-41 - dado compra negada quando compartilhar entao nada e preparado`() =
        runTest {
            val declined = aPurchase(status = PurchaseStatus.Declined)

            val result = useCase(declined)(declined.id)

            assertEquals(DataResult.Failure(TicketingError.TicketsUnavailable), result)
            assertEquals(0, files.prepared.size)
        }

    @Test
    fun `AC-42 - dado compartilhar entao o pacote leva so as imagens, sem destinatario`() =
        runTest {
            val bundle = (useCase(approved)(approved.id) as DataResult.Success).value!!

            assertEquals(listOf("content://test/tickets/1.png", "content://test/tickets/2.png"), bundle.uris)
        }

    @Test
    fun `AC-39 - dado impressao em andamento quando compartilhar entao a acao e ignorada`() =
        runTest {
            printer.emit(PrintStatus.Printing(approved.id, printed = 0, total = 2))

            val result = useCase(approved)(approved.id)

            assertNull((result as DataResult.Success).value)
            assertEquals(0, files.prepared.size)
        }

    @Test
    fun `AC-43 - dado falha ao gerar a imagem quando compartilhar entao falha sem alterar a compra`() =
        runTest {
            files.fail = true

            val result = useCase(approved)(approved.id)

            assertEquals(DataResult.Failure(TicketingError.TicketGenerationFailed), result)
        }
}
