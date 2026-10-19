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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.tickets

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.TicketsRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeQrEncoder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketFiles
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketPrinter
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.GenerateTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.LoadTicketSetUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.PrintTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ShareTicketsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.SweepTicketFilesUseCase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** SCR-05: tickets, print and share as states (spec 002, AC-34 to AC-44). */
internal class TicketsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val approved = aPurchase(reference = "ref-approved", quantity = 3, status = PurchaseStatus.Approved)
    private val printer = FakeTicketPrinter()
    private val files = FakeTicketFiles()
    private val encoder = FakeQrEncoder()

    private fun viewModel(
        vararg purchases: Purchase,
        id: String = approved.id,
        repository: FakePurchaseRepository = FakePurchaseRepository(purchases.toList()),
    ): TicketsViewModel {
        val generate = GenerateTicketsUseCase()
        val load = LoadTicketSetUseCase(repository, generate)
        return TicketsViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TicketsRoute.serializer().descriptor.getElementName(0) to id)),
            observeTickets = ObserveTicketsUseCase(repository, generate, encoder, mainDispatcher.dispatcher),
            printTickets = PrintTicketsUseCase(load, repository, printer),
            shareTickets = ShareTicketsUseCase(load, files, printer),
            printer = printer,
            sweepFiles = SweepTicketFilesUseCase(files, printer),
        )
    }

    private fun TicketsUiState.content(): TicketsUiState.Content =
        assertInstanceOf(TicketsUiState.Content::class.java, this)

    @Test
    fun `AC-34 - dado compra aprovada de 3 quando abrir entao mostra Loading e depois um QR Code por ingresso`() =
        runTest {
            viewModel(approved).uiState.test {
                assertEquals(TicketsUiState.Loading, awaitItem())
                val content = awaitItem().content()

                assertEquals(listOf(1, 2, 3), content.tickets.map { it.ordinal })
                assertTrue(content.tickets.all { it.total == 3 })
                assertEquals(approved.eventName, content.event.name)
                assertEquals(TicketActionState.Idle, content.action)
            }
        }

    @Test
    fun `AC-35 - dado que a geracao falha quando abrir entao Error e Tentar novamente refaz a leitura`() =
        runTest {
            encoder.fail = true
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                assertEquals(TicketsUiState.Loading, awaitItem())
                assertEquals(TicketsUiState.Error, awaitItem())

                encoder.fail = false
                viewModel.onEvent(TicketsUiEvent.OnRetry)

                assertEquals(TicketsUiState.Loading, awaitItem())
                assertInstanceOf(TicketsUiState.Content::class.java, awaitItem())
            }
        }

    @Test
    fun `AC-36 - dado compra que nao esta aprovada ou nao existe quando abrir entao Error sem nenhum QR Code`() =
        runTest {
            val cancelled = aPurchase(reference = "ref-cancelled", status = PurchaseStatus.Cancelled)

            viewModel(cancelled, id = cancelled.id).uiState.test {
                assertEquals(TicketsUiState.Loading, awaitItem())
                assertEquals(TicketsUiState.Error, awaitItem())
            }
            viewModel(cancelled, id = "nao-existe").uiState.test {
                assertEquals(TicketsUiState.Loading, awaitItem())
                assertEquals(TicketsUiState.Error, awaitItem())
            }
            assertEquals(0, encoder.encoded.size)
        }

    @Test
    fun `AC-37 - dado compra Falhou por nao encontrado quando aprovada depois entao mostra os ingressos`() =
        runTest {
            val failed =
                aPurchase(
                    reference = "ref-late",
                    status = PurchaseStatus.Failed,
                    failureReason = FailureReason.NotFound,
                )
            val repository = FakePurchaseRepository(listOf(failed))

            viewModel(id = failed.id, repository = repository).uiState.test {
                assertEquals(TicketsUiState.Loading, awaitItem())
                assertEquals(TicketsUiState.Error, awaitItem())

                repository.update(failed.copy(status = PurchaseStatus.Approved, failureReason = null))

                assertInstanceOf(TicketsUiState.Content::class.java, awaitItem())
            }
        }

    @Test
    fun `AC-38 - dado ingressos abertos quando imprimir entao pede a impressao desde o primeiro e mostra Printing`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                assertEquals(TicketsUiState.Loading, awaitItem())
                awaitItem()

                viewModel.onEvent(TicketsUiEvent.OnPrint)

                assertEquals(TicketActionState.Printing(0, 3), awaitItem().content().action)
                assertEquals(0, printer.requests.single().second)
            }
        }

    @Test
    fun `AC-39 - dado impressao em andamento quando imprimir ou compartilhar entao as duas acoes sao ignoradas`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                viewModel.onEvent(TicketsUiEvent.OnPrint)
                awaitItem()

                viewModel.onEvent(TicketsUiEvent.OnPrint)
                viewModel.onEvent(TicketsUiEvent.OnShare)
                advanceUntilIdle()

                assertEquals(1, printer.requests.size)
                assertEquals(0, files.prepared.size)
                expectNoEvents()
            }
        }

    @Test
    fun `AC-40 - dado sem papel depois do 1o quando tentar de novo entao a impressao retoma do ingresso 2`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                printer.emit(PrintStatus.Failed(approved.id, PrintFailureReason.NoPaper, printed = 1, total = 3))
                val failure = TicketActionState.PrintFailed(PrintFailureReason.NoPaper, 1, 3)
                assertEquals(failure, awaitItem().content().action)

                viewModel.onEvent(TicketsUiEvent.OnPrint)
                awaitItem()

                assertEquals(1, printer.requests.single().second)
            }
        }

    @Test
    fun `AC-40 - dado falha de impressao de outra compra quando abrir esta tela entao as acoes estao livres`() =
        runTest {
            printer.emit(PrintStatus.Failed("outra", PrintFailureReason.PrinterError, printed = 2, total = 3))

            viewModel(approved).uiState.test {
                awaitItem()
                assertEquals(TicketActionState.Idle, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-40 - dado app Cielo ausente quando imprimir entao nada e enviado e a tela mostra a falha`() =
        runTest {
            printer.terminalAvailable = false
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                viewModel.onEvent(TicketsUiEvent.OnPrint)

                val failed = TicketActionState.PrintFailed(PrintFailureReason.TerminalAppUnavailable, 0, 3)
                assertEquals(failed, awaitItem().content().action)
                assertEquals(0, printer.requests.size)
            }
        }

    @Test
    fun `AC-40 - dado compra em andamento quando imprimir entao a tela mostra a falha em vez de ficar muda`() =
        runTest {
            val pending = aPurchase(reference = "ref-pending", status = PurchaseStatus.Pending)
            val viewModel = viewModel(approved, pending)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                viewModel.onEvent(TicketsUiEvent.OnPrint)

                val failed = TicketActionState.PrintFailed(PrintFailureReason.Unknown, 0, 3)
                assertEquals(failed, awaitItem().content().action)
                assertEquals(0, printer.requests.size)
            }
        }

    @Test
    fun `AC-41 - dado ingressos abertos quando compartilhar entao sinaliza as imagens e consome o sinal`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onEvent(TicketsUiEvent.OnShare)
                val ready = awaitItem().content().action

                assertEquals(3, (ready as TicketActionState.ShareReady).bundle.uris.size)
                viewModel.onEvent(TicketsUiEvent.OnShareLaunched)
                assertEquals(TicketActionState.Idle, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-43 - dado falha ao preparar as imagens quando compartilhar entao ShareFailed e Tentar novamente repete`() =
        runTest {
            files.fail = true
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                viewModel.onEvent(TicketsUiEvent.OnShare)
                assertEquals(TicketActionState.ShareFailed, awaitItem().content().action)

                files.fail = false
                viewModel.onEvent(TicketsUiEvent.OnRetryAction)

                assertInstanceOf(TicketActionState.ShareReady::class.java, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-43 - dado falha de impressao quando compartilhar entao a falha anterior e substituida`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                printer.emit(PrintStatus.Failed(approved.id, PrintFailureReason.PrinterError, printed = 0, total = 3))
                awaitItem()

                viewModel.onEvent(TicketsUiEvent.OnShare)

                assertEquals(1, printer.acknowledged)
                assertEquals(TicketActionState.Idle, awaitItem().content().action)
                assertInstanceOf(TicketActionState.ShareReady::class.java, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-44 - dado impressao concluida quando a Cielo confirma entao Printed e depois volta a Idle`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                printer.emit(PrintStatus.Done(approved.id, total = 3))
                assertEquals(TicketActionState.Printed, awaitItem().content().action)

                viewModel.onEvent(TicketsUiEvent.OnActionDismissed)

                assertEquals(TicketActionState.Idle, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-44 - dado Printed de uma visita anterior quando abrir a tela de novo entao o aviso nao volta`() =
        runTest {
            printer.emit(PrintStatus.Done(approved.id, total = 3))

            viewModel(approved).uiState.test {
                awaitItem()
                assertEquals(TicketActionState.Idle, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-43 - dado que o sistema nao abre o menu de compartilhar quando avisar entao ShareFailed`() =
        runTest {
            val viewModel = viewModel(approved)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()
                viewModel.onEvent(TicketsUiEvent.OnShare)
                awaitItem()
                viewModel.onEvent(TicketsUiEvent.OnShareLaunched)
                assertEquals(TicketActionState.Idle, awaitItem().content().action)

                viewModel.onEvent(TicketsUiEvent.OnShareLaunchFailed)

                assertEquals(TicketActionState.ShareFailed, awaitItem().content().action)
            }
        }

    @Test
    fun `AC-42 - dado que a tela de ingressos abre quando iniciar entao limpa as imagens que sobraram`() =
        runTest {
            viewModel(approved).uiState.test {
                awaitItem()
                awaitItem()
                assertEquals(1, files.swept)
            }
        }
}
