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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase.RecordingLogger
import com.rafaeloliveira.bilheteria.feature.ticketing.data.ticket.PrintImageStore
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * The print job state machine (AC-38 to AC-40, AC-44): one ticket per call, resume, timeout and cleanup. It runs in a
 * `backgroundScope`: `advanceUntilIdle` ignores background work, so each step is driven with `runCurrent` (same
 * virtual instant) and the timeout with `advanceTimeBy`.
 */
class CieloPrintCoordinatorTest {
    private val events = mutableListOf<String>()
    private val launcher = RecordingLauncher()
    private val session = RecordingSession(events)
    private val images = FakeImageStore()
    private val logger = RecordingLogger()

    private val reference = "91ccb5d9-ccf6-4471-bfe2-413d5bc48637"
    private val set =
        TicketSet(
            purchaseId = reference,
            eventName = "Noite de Jazz no Parque",
            eventStartsAt = Instant.parse("2026-11-15T23:00:00Z"),
            venue = "Concha Acústica Municipal",
            tickets = (1..3).map { Ticket(it, 3, TicketCode("bilheteria:1:$reference:$it")) },
        )

    private fun TestScope.coordinator() =
        CieloPrintCoordinator(
            opener = DeepLinkOpener(launcher, session, logger),
            encoder = CieloPrintEncoder(),
            images = images,
            session = session,
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            logger = logger,
        )

    private fun printedPaths(): List<String> =
        launcher.uris.map {
            Regex("\"value\":\\[\"([^\"]*)\"").find(decodePrintRequest(it))!!.groupValues[1]
        }

    @Test
    fun `AC-38 - dado compra de 3 quando imprimir entao envia um ingresso por chamada e so avanca com a confirmacao`() =
        runTest {
            val printer = coordinator()

            assertEquals(DataResult.Success(Unit), printer.print(set))
            assertEquals(PrintStatus.Printing(reference, printed = 0, total = 3), printer.status.value)
            assertTrue(printer.awaitingResult)
            assertEquals(listOf("/img/1.png"), printedPaths())

            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()
            assertEquals(PrintStatus.Printing(reference, printed = 1, total = 3), printer.status.value)
            assertEquals(listOf("/img/1.png", "/img/2.png"), printedPaths())

            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()
            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()
            assertEquals(PrintStatus.Done(reference, total = 3), printer.status.value)
            assertEquals(listOf("/img/1.png", "/img/2.png", "/img/3.png"), printedPaths())
            assertFalse(printer.awaitingResult)
        }

    @Test
    fun `AC-44 - dado impressao concluida quando reconhecer entao volta a Idle e libera nova impressao`() =
        runTest {
            val printer = coordinator()
            printer.print(set)
            repeat(3) {
                printer.onPrintOutcome(PrintOutcome.Success)
                testScheduler.runCurrent()
            }

            printer.acknowledge()

            assertEquals(PrintStatus.Idle, printer.status.value)
            assertEquals(DataResult.Success(Unit), printer.print(set))
            assertEquals(PrintStatus.Printing(reference, printed = 0, total = 3), printer.status.value)
        }

    @Test
    fun `AC-39 - dado impressao em andamento quando pedir de novo entao nenhuma nova chamada e enviada`() =
        runTest {
            val printer = coordinator()
            printer.print(set)

            val again = printer.print(set)

            assertEquals(DataResult.Success(Unit), again)
            assertEquals(1, launcher.uris.size)
        }

    @Test
    fun `AC-40 - dado sem papel depois do 1o quando tentar de novo entao so os ingressos 2 e 3 sao enviados`() =
        runTest {
            val printer = coordinator()
            printer.print(set)
            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()

            printer.onPrintOutcome(PrintOutcome.NoPaper)
            testScheduler.runCurrent()

            assertEquals(
                PrintStatus.Failed(reference, PrintFailureReason.NoPaper, printed = 1, total = 3),
                printer.status.value,
            )
            assertFalse(printer.awaitingResult)

            printer.print(set, from = 1)
            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()
            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()

            assertEquals(listOf("/img/1.png", "/img/2.png", "/img/2.png", "/img/3.png"), printedPaths())
            assertEquals(PrintStatus.Done(reference, total = 3), printer.status.value)
        }

    @Test
    fun `AC-40 - dado respostas de erro quando chegarem entao cada uma vira o motivo certo`() =
        runTest {
            val expected =
                mapOf(
                    PrintOutcome.PrinterError to PrintFailureReason.PrinterError,
                    PrintOutcome.FileUnreadable to PrintFailureReason.Unknown,
                    PrintOutcome.Unreadable to PrintFailureReason.Unknown,
                )

            expected.forEach { (outcome, reason) ->
                val printer = coordinator()
                printer.print(set)
                printer.onPrintOutcome(outcome)
                testScheduler.runCurrent()

                assertEquals(
                    PrintStatus.Failed(reference, reason, printed = 0, total = 3),
                    printer.status.value,
                    "$outcome",
                )
            }
        }

    @Test
    fun `AC-40 - dado app Cielo ausente quando imprimir entao falha com terminal indisponivel e nada e enviado`() =
        runTest {
            launcher.resolved = null
            val printer = coordinator()

            val result = printer.print(set)

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(
                PrintStatus.Failed(reference, PrintFailureReason.TerminalAppUnavailable, printed = 0, total = 3),
                printer.status.value,
            )
            assertTrue(launcher.uris.isEmpty())
            assertEquals(listOf("/img/1.png"), images.deleted)
        }

    @Test
    fun `AC-40 - dado aparelho que nao imprime imagem quando imprimir entao falha com terminal indisponivel`() =
        runTest {
            images.failWith = TicketingError.TerminalAppUnavailable
            val printer = coordinator()

            val result = printer.print(set)

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(PrintFailureReason.TerminalAppUnavailable, (printer.status.value as PrintStatus.Failed).reason)
        }

    @Test
    fun `AC-40 - dado falha ao gravar a imagem quando imprimir entao falha como desconhecido`() =
        runTest {
            images.failWith = TicketingError.TicketGenerationFailed
            val printer = coordinator()

            printer.print(set)

            assertEquals(PrintFailureReason.Unknown, (printer.status.value as PrintStatus.Failed).reason)
            assertTrue(launcher.uris.isEmpty())
        }

    @Test
    fun `AC-40 - dado que a Cielo nao responde em 15 s quando o tempo passar entao falha com Timeout`() =
        runTest {
            val printer = coordinator()
            printer.print(set)

            testScheduler.advanceTimeBy(CieloPrintCoordinator.STEP_TIMEOUT_MILLIS + 1)
            testScheduler.runCurrent()

            assertEquals(
                PrintStatus.Failed(reference, PrintFailureReason.Timeout, printed = 0, total = 3),
                printer.status.value,
            )
            assertFalse(printer.awaitingResult)
            assertEquals(listOf("/img/1.png"), images.deleted)

            printer.onPrintOutcome(PrintOutcome.Success) // late answer: dropped
            testScheduler.runCurrent()
            assertEquals(PrintFailureReason.Timeout, (printer.status.value as PrintStatus.Failed).reason)
        }

    @Test
    fun `AC-40 - dado passo aguardando quando faltar 1 ms para o limite entao ainda esta imprimindo`() =
        runTest {
            val printer = coordinator()
            printer.print(set)

            testScheduler.advanceTimeBy(CieloPrintCoordinator.STEP_TIMEOUT_MILLIS - 1)
            testScheduler.runCurrent()

            assertEquals(PrintStatus.Printing(reference, printed = 0, total = 3), printer.status.value)
            assertTrue(printer.awaitingResult)
        }

    @Test
    fun `AC-40 - dado 1o ingresso impresso quando o 2o nao responde entao Timeout com 1 impresso`() =
        runTest {
            val printer = coordinator()
            printer.print(set)
            testScheduler.advanceTimeBy(CieloPrintCoordinator.STEP_TIMEOUT_MILLIS - 1_000)
            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()

            testScheduler.advanceTimeBy(CieloPrintCoordinator.STEP_TIMEOUT_MILLIS - 1_000) // the timer restarted
            testScheduler.runCurrent()
            assertEquals(PrintStatus.Printing(reference, printed = 1, total = 3), printer.status.value)

            testScheduler.advanceTimeBy(2_000)
            testScheduler.runCurrent()

            val expected = PrintStatus.Failed(reference, PrintFailureReason.Timeout, printed = 1, total = 3)
            assertEquals(expected, printer.status.value)
        }

    @Test
    fun `AC-40 - dado o envio do passo quando a Cielo abre entao o coordenador ja espera a resposta`() =
        runTest {
            lateinit var printer: CieloPrintCoordinator
            var waitingWhenSent = false
            launcher.beforeLaunchReturns = { waitingWhenSent = printer.awaitingResult }
            printer = coordinator()

            printer.print(set)

            assertTrue(waitingWhenSent)
        }

    @Test
    fun `AC-40 - dado erro inesperado ao gravar a imagem quando imprimir entao Failed Unknown`() =
        runTest {
            images.crash = true
            val printer = coordinator()

            val result = printer.print(set)

            assertTrue(result is DataResult.Failure)
            val failed = PrintStatus.Failed(reference, PrintFailureReason.Unknown, printed = 0, total = 3)
            assertEquals(failed, printer.status.value)
            assertFalse(printer.awaitingResult)
        }

    @Test
    fun `AC-39 - dado que a tela sai durante a gravacao quando cancelar a chamada entao o trabalho nao fica preso`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            images.blockWrite = gate
            val printer = coordinator()
            val caller = launch { printer.print(set) }
            testScheduler.runCurrent()

            caller.cancel() // the screen left while the image was being written
            gate.complete(Unit)
            testScheduler.runCurrent()

            assertTrue(printer.awaitingResult, "the step was sent, so the answer is awaited")
            assertEquals(1, launcher.uris.size)

            testScheduler.advanceTimeBy(CieloPrintCoordinator.STEP_TIMEOUT_MILLIS + 1)
            testScheduler.runCurrent()

            assertEquals(PrintFailureReason.Timeout, (printer.status.value as PrintStatus.Failed).reason)
            assertEquals(listOf("/img/1.png"), images.deleted)
        }

    @Test
    fun `AC-38 - dado resposta sem impressao aguardando quando chegar entao e ignorada`() =
        runTest {
            val printer = coordinator()

            printer.onPrintOutcome(PrintOutcome.Success)
            testScheduler.runCurrent()

            assertEquals(PrintStatus.Idle, printer.status.value)
        }

    @Test
    fun `AC-38 - dado cada passo quando termina entao a imagem e apagada e o servico para ao fim`() =
        runTest {
            val printer = coordinator()
            printer.print(set)
            repeat(3) {
                printer.onPrintOutcome(PrintOutcome.Success)
                testScheduler.runCurrent()
            }

            assertEquals(listOf("/img/1.png", "/img/2.png", "/img/3.png"), images.deleted)
            assertEquals("service-stop", events.last())
        }

    @Test
    fun `AC-38 - dado impressao quando registrar logs entao nenhum leva caminho referencia ou resposta`() =
        runTest {
            val printer = coordinator()
            printer.print(set)
            printer.onPrintOutcome(PrintOutcome.NoPaper)
            testScheduler.runCurrent()

            val all = logger.infos + logger.warnings + logger.errors
            assertTrue(all.isNotEmpty())
            assertFalse(all.joinToString(" ").let { it.contains("/img/") || it.contains(reference) })
        }

    private class RecordingLauncher : TerminalAppLauncher {
        var resolved: String? = "br.com.cielosmart.orderservice"
        var beforeLaunchReturns: () -> Unit = {}
        val uris = mutableListOf<String>()

        override fun resolvePackage(uri: String): String? = resolved

        override fun launch(
            uri: String,
            packageName: String,
        ): Boolean {
            uris += uri
            beforeLaunchReturns()
            return true
        }
    }

    private class RecordingSession(
        private val events: MutableList<String>,
    ) : PaymentSession {
        override fun start() {
            events += "service-start"
        }

        override fun stop() {
            events += "service-stop"
        }
    }

    private class FakeImageStore : PrintImageStore {
        var failWith: TicketingError? = null
        var blockWrite: CompletableDeferred<Unit>? = null
        var crash = false
        val deleted = mutableListOf<String>()

        override suspend fun write(
            set: TicketSet,
            ticket: Ticket,
        ): DataResult<String> {
            blockWrite?.await()
            check(!crash) { "unexpected" }
            return failWith?.let { DataResult.Failure(it) } ?: DataResult.Success("/img/${ticket.ordinal}.png")
        }

        override fun delete(path: String) {
            deleted += path
        }

        override fun sweepStale() = Unit
    }
}
