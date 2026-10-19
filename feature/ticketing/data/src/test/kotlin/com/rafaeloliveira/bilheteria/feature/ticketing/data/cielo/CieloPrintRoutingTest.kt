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

import com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase.RecordingLogger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.HandlePaymentOutcomeUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.PurchaseWriteLock
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.RecoverPendingPurchaseUseCase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.ZoneOffset

internal class FakePrintResultSink(
    override var awaitingResult: Boolean = false,
) : PrintResultSink {
    val outcomes = mutableListOf<PrintOutcome>()

    override fun onPrintOutcome(outcome: PrintOutcome) {
        outcomes += outcome
    }
}

/** AC-40 and the cross-routing risk (plan R2): a print answer never changes a purchase. */
class CieloPrintRoutingTest {
    private val parser = CieloResponseParser()
    private val clock = Clock.fixed(TEST_NOW.plusSeconds(30), ZoneOffset.UTC)
    private val writeLock = PurchaseWriteLock()
    private val gateway = FakePaymentGateway()
    private val logger = RecordingLogger()
    private val sink = FakePrintResultSink()

    private fun print(json: String) = cieloBase64(json)

    private fun handler(
        repository: FakePurchaseRepository,
        scope: TestScope,
    ) = CieloCallbackHandler(
        parser = parser,
        handlePaymentOutcome =
            HandlePaymentOutcomeUseCase(
                repository = repository,
                gateway = gateway,
                recover = RecoverPendingPurchaseUseCase(repository, gateway, clock, writeLock),
                clock = clock,
                logger = logger,
                lock = writeLock,
            ),
        printSink = sink,
        scope = scope,
        logger = logger,
    )

    @Test
    fun `AC-38 - dado resposta de impressao com sucesso quando interpretar entao e Success e certa`() {
        val response = parser.parsePrint(print("""{"code":0,"message":"SUCCESS"}"""))

        assertEquals(PrintOutcome.Success, response?.outcome)
        assertTrue(response!!.certain)
    }

    @Test
    fun `AC-40 - dado resposta sem papel quando interpretar entao e NoPaper e certa`() {
        val response = parser.parsePrint(print("""{"code":1,"message":"WITHOUT PAPER"}"""))

        assertEquals(PrintOutcome.NoPaper, response?.outcome)
        assertTrue(response!!.certain)
    }

    @Test
    fun `AC-40 - dado erro da impressora quando interpretar entao e PrinterError`() {
        val response = parser.parsePrint(print("""{"code":2,"message":"java.lang.Throwable: PRINT ERROR"}"""))

        assertEquals(PrintOutcome.PrinterError, response?.outcome)
    }

    @Test
    fun `AC-40 - dado arquivo ilegivel com reason quando interpretar entao e FileUnreadable e ambigua`() {
        val response = parser.parsePrint(print("""{"code":2,"reason":"open failed: EACCES (Permission denied)"}"""))

        assertEquals(PrintOutcome.FileUnreadable, response?.outcome)
        assertTrue(!response!!.certain)
    }

    @Test
    fun `AC-40 - dado resposta de pagamento aprovado quando interpretar como impressao entao nao e de impressao`() {
        assertNull(parser.parsePrint(cieloBase64(fixture("payment_approved.json"))))
    }

    @Test
    fun `AC-40 - dado lixo ou parametro ausente quando interpretar como impressao entao nao e de impressao`() {
        assertNull(parser.parsePrint("isto-nao-e-base64-nem-json"))
        assertNull(parser.parsePrint(null))
    }

    @Test
    fun `AC-40 - dado impressao aguardando e sem papel quando o callback chegar entao a compra nao muda`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            sink.awaitingResult = true
            var done = false

            handler(repository, this).handle(print("""{"code":1,"message":"WITHOUT PAPER"}""")) { done = true }
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(PrintOutcome.NoPaper), sink.outcomes)
            assertEquals(PurchaseStatus.Pending, repository.all.single().status)
            assertTrue(done)
        }

    @Test
    fun `AC-40 - dado impressao aguardando e erro de arquivo quando o callback chegar entao nao cancela a compra`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            sink.awaitingResult = true

            handler(repository, this).handle(print("""{"code":2,"reason":"open failed: EACCES"}""")) {}
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(PrintOutcome.FileUnreadable), sink.outcomes)
            assertEquals(PurchaseStatus.Pending, repository.all.single().status)
        }

    @Test
    fun `AC-40 - dado resposta de impressao sem trabalho quando o callback chegar entao e descartada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            var done = false

            handler(repository, this).handle(print("""{"code":1,"message":"WITHOUT PAPER"}""")) { done = true }
            testScheduler.advanceUntilIdle()

            assertTrue(sink.outcomes.isEmpty())
            assertEquals(PurchaseStatus.Pending, repository.all.single().status)
            assertEquals(listOf("print_result_discarded"), logger.warnings)
            assertTrue(done)
        }

    @Test
    fun `AC-40 - dado cancelamento de pagamento sem impressao em andamento quando chegar entao segue o pagamento`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            handler(repository, this).handle(print("""{"code":1,"reason":"CANCELADO PELO USUARIO"}""")) {}
            testScheduler.advanceUntilIdle()

            assertEquals(PurchaseStatus.Cancelled, repository.all.single().status)
            assertTrue(sink.outcomes.isEmpty())
        }

    @Test
    fun `AC-09 - dado pagamento aprovado com impressao aguardando quando o callback chegar entao segue o pagamento`() =
        runTest {
            val reference = "0b8f2d1c-1111-4222-8333-444455556666"
            val repository = FakePurchaseRepository(listOf(aPurchase(reference = reference)))
            sink.awaitingResult = true

            handler(repository, this).handle(cieloBase64(fixture("payment_approved.json"))) {}
            testScheduler.advanceUntilIdle()

            assertEquals(PurchaseStatus.Approved, repository.all.single().status)
            assertTrue(sink.outcomes.isEmpty())
            assertNotNull(repository.all.single().transaction)
        }
}
