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
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.LookupOrder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Clock
import java.time.ZoneOffset

class HandlePaymentOutcomeUseCaseTest {
    private val now = TEST_NOW.plusSeconds(30)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val gateway = FakePaymentGateway()
    private val logger = RecordingLogger()
    private val reference = PurchaseReference("91ccb5d9-ccf6-4471-bfe2-413d5bc48637")
    private val total = Money(10_000)

    private fun useCase(repository: FakePurchaseRepository) =
        HandlePaymentOutcomeUseCase(
            repository = repository,
            gateway = gateway,
            recover = RecoverPendingPurchaseUseCase(repository, gateway, clock),
            clock = clock,
            logger = logger,
        )

    private fun approved(
        reference: PurchaseReference? = this.reference,
        amount: Money = total,
    ) = PaymentOutcome.Approved(reference, amount, aTransaction())

    private fun order(approved: PaymentOutcome.Approved? = null) = LookupOrder(reference, approved)

    private fun otherOrders() =
        listOf(LookupOrder(PurchaseReference("other-1"), null), LookupOrder(PurchaseReference("other-2"), approved()))

    private fun recovering() = aPurchase(status = PurchaseStatus.Recovering)

    @Test
    fun `AC-09 - dado retorno autorizado quando processar entao registra aprovada com a transacao`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCase(repository)(approved())

            val expected =
                aPurchase(status = PurchaseStatus.Approved, transaction = aTransaction()).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-09 - dado compra em recuperacao quando chega retorno autorizado tardio entao registra aprovada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))

            useCase(repository)(approved())

            assertEquals(PurchaseStatus.Approved, repository.all.single().status)
            assertEquals(aTransaction(), repository.all.single().transaction)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-09 - dado retorno autorizado sem reference quando processar entao aplica na compra aberta`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            useCase(repository)(approved(reference = null))

            assertEquals(PurchaseStatus.Approved, repository.all.single().status)
        }

    @Test
    fun `AC-10 - dado codigo 1 quando processar entao registra cancelada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCase(repository)(PaymentOutcome.Cancelled(reference))

            val expected = aPurchase(status = PurchaseStatus.Cancelled).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-11 - dado codigo 3 quando processar entao registra negada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCase(repository)(PaymentOutcome.Declined(reference))

            val expected = aPurchase(status = PurchaseStatus.Declined).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(1, gateway.finished)
        }

    @ParameterizedTest
    @EnumSource(value = FailureReason::class, names = ["Generic", "Authentication"])
    fun `AC-13 - dado codigo 2 ou 4 quando processar entao registra falhou com o motivo`(reason: FailureReason) =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCase(repository)(PaymentOutcome.Failed(reference, reason))

            val expected =
                aPurchase(status = PurchaseStatus.Failed, failureReason = reason).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-14 - dado resposta ilegivel quando processar entao recupera e consulta a Cielo sem aprovar`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCase(repository)(PaymentOutcome.Unreadable)

            val expected = aPurchase(status = PurchaseStatus.Recovering).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(listOf(0), gateway.lookupRequests)
            assertEquals(0, gateway.finished)
        }

    @Test
    fun `AC-14 - dado resposta ilegivel e app Cielo ausente quando processar entao falha sem cobranca`() =
        runTest {
            gateway.terminalAvailable = false
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            val result = useCase(repository)(PaymentOutcome.Unreadable)

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(PurchaseStatus.Failed, repository.all.single().status)
            assertEquals(FailureReason.TerminalAppUnavailable, repository.all.single().failureReason)
        }

    @Test
    fun `AC-15 - dado pagina com pedido aprovado e valor igual quando processar entao registra aprovada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))
            val page = PaymentOutcome.LookupPage(0, 1, otherOrders() + order(approved()))

            val result = useCase(repository)(page)

            val expected =
                aPurchase(status = PurchaseStatus.Approved, transaction = aTransaction()).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-15 - dado pedido encontrado numa pagina anterior quando processar entao registra aprovada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))
            val useCase = useCase(repository)

            useCase(PaymentOutcome.LookupPage(0, 10, otherOrders()))
            useCase(PaymentOutcome.LookupPage(9, 10, otherOrders()))
            useCase(PaymentOutcome.LookupPage(8, 10, listOf(order(approved()))))

            assertEquals(PurchaseStatus.Approved, repository.all.single().status)
            assertEquals(listOf(9, 8), gateway.lookupRequests)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-15 - dado pedido encontrado sem pagamento aprovado quando processar entao falhou nao encontrado`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))

            useCase(repository)(PaymentOutcome.LookupPage(0, 5, listOf(order(approved = null))))

            val expected =
                aPurchase(status = PurchaseStatus.Failed, failureReason = FailureReason.NotFound).copy(updatedAt = now)
            assertEquals(listOf(expected), repository.all)
            assertTrue(gateway.lookupRequests.isEmpty())
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-15 - dado pedido aprovado com valor diferente quando processar entao nao aprova`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))
            val page = PaymentOutcome.LookupPage(0, 1, listOf(order(approved(amount = Money(1)))))

            useCase(repository)(page)

            assertEquals(PurchaseStatus.Failed, repository.all.single().status)
            assertEquals(FailureReason.NotFound, repository.all.single().failureReason)
            assertEquals(null, repository.all.single().transaction)
        }

    @Test
    fun `AC-15 - dado pedido nao encontrado nas 4 ultimas paginas quando processar entao falhou nao encontrado`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))
            val useCase = useCase(repository)

            listOf(0, 9, 8, 7).forEach { useCase(PaymentOutcome.LookupPage(it, 10, otherOrders())) }
            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
            useCase(PaymentOutcome.LookupPage(6, 10, otherOrders()))

            val purchase = repository.all.single()
            assertEquals(PurchaseStatus.Failed, purchase.status)
            assertEquals(FailureReason.NotFound, purchase.failureReason)
            assertEquals(listOf(9, 8, 7, 6), gateway.lookupRequests)
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-15 - dado total de paginas pequeno quando nao encontrar entao varre ate a pagina 1 e falha`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))
            val useCase = useCase(repository)

            useCase(PaymentOutcome.LookupPage(0, 3, otherOrders()))
            useCase(PaymentOutcome.LookupPage(2, 3, otherOrders()))
            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
            useCase(PaymentOutcome.LookupPage(1, 3, otherOrders()))

            assertEquals(listOf(2, 1), gateway.lookupRequests)
            assertEquals(PurchaseStatus.Failed, repository.all.single().status)
            assertEquals(FailureReason.NotFound, repository.all.single().failureReason)
        }

    @Test
    fun `AC-15 - dado uma unica pagina sem o pedido quando processar entao falhou sem nova consulta`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))

            useCase(repository)(PaymentOutcome.LookupPage(0, 1, otherOrders()))

            assertEquals(FailureReason.NotFound, repository.all.single().failureReason)
            assertTrue(gateway.lookupRequests.isEmpty())
            assertEquals(1, gateway.finished)
        }

    @Test
    fun `AC-15 - dado listagem vazia quando processar entao falhou nao encontrado`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(recovering()))

            useCase(repository)(PaymentOutcome.LookupPage(0, 0, emptyList()))

            assertEquals(FailureReason.NotFound, repository.all.single().failureReason)
            assertTrue(gateway.lookupRequests.isEmpty())
        }

    @Test
    fun `AC-15 - dado falha ao pedir a proxima pagina quando processar entao devolve a falha e segue em recuperacao`() =
        runTest {
            gateway.terminalAvailable = false
            val repository = FakePurchaseRepository(listOf(recovering()))

            val result = useCase(repository)(PaymentOutcome.LookupPage(0, 10, otherOrders()))

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(PurchaseStatus.Recovering, repository.all.single().status)
            assertEquals(0, gateway.finished)
        }

    @Test
    fun `AC-09 - dado reference divergente quando chega retorno autorizado entao descarta sem mudar a compra`() =
        runTest {
            val purchase = aPurchase()
            val repository = FakePurchaseRepository(listOf(purchase))
            val forged = approved(reference = PurchaseReference("forged-reference"))

            val result = useCase(repository)(forged)

            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(purchase), repository.all)
            assertEquals(0, gateway.finished)
            assertEquals(listOf("callback_discarded reason=reference_mismatch"), logger.warnings)
        }

    @Test
    fun `AC-10 - dado reference divergente quando chega cancelamento entao descarta sem mudar a compra`() =
        runTest {
            val purchase = aPurchase()
            val repository = FakePurchaseRepository(listOf(purchase))

            useCase(repository)(PaymentOutcome.Cancelled(PurchaseReference("forged-reference")))

            assertEquals(listOf(purchase), repository.all)
            assertEquals(0, gateway.finished)
        }

    @Test
    fun `AC-09 - dado valor pago divergente do total quando chega retorno autorizado entao descarta`() =
        runTest {
            val purchase = aPurchase()
            val repository = FakePurchaseRepository(listOf(purchase))

            val result = useCase(repository)(approved(amount = Money(100)))

            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(purchase), repository.all)
            assertEquals(0, gateway.finished)
            assertEquals(listOf("callback_discarded reason=amount_mismatch"), logger.warnings)
        }

    @Test
    fun `AC-09 - dado nenhuma compra aberta quando chega retorno autorizado entao descarta sem registrar`() =
        runTest {
            val closed = aPurchase(status = PurchaseStatus.Declined)
            val repository = FakePurchaseRepository(listOf(closed))

            val result = useCase(repository)(approved())

            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(closed), repository.all)
            assertEquals(0, gateway.finished)
            assertEquals(listOf("callback_discarded reason=no_open_purchase"), logger.warnings)
        }

    @Test
    fun `AC-14 - dado nenhuma compra aberta quando chega resposta ilegivel entao descarta sem consultar`() =
        runTest {
            val repository = FakePurchaseRepository()

            val result = useCase(repository)(PaymentOutcome.Unreadable)

            assertEquals(DataResult.Success(Unit), result)
            assertTrue(gateway.lookupRequests.isEmpty())
            assertEquals(listOf("callback_discarded reason=no_open_purchase"), logger.warnings)
        }

    @Test
    fun `AC-15 - dado compra pendente quando chega pagina de consulta forjada entao descarta sem mudar a compra`() =
        runTest {
            val purchase = aPurchase(status = PurchaseStatus.Pending)
            val repository = FakePurchaseRepository(listOf(purchase))
            val forged = PaymentOutcome.LookupPage(0, 1, listOf(order(approved())))

            val result = useCase(repository)(forged)

            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(purchase), repository.all)
            assertEquals(0, gateway.finished)
            assertTrue(gateway.lookupRequests.isEmpty())
            assertEquals(listOf("callback_discarded reason=not_recovering"), logger.warnings)
        }

    @Test
    fun `AC-09 - dado retorno processado quando registrar logs entao so tem evento e reference`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))

            useCase(repository)(approved())

            val transaction = aTransaction()
            val logs = logger.all.joinToString("\n")
            assertTrue(logs.contains("reference=${reference.value}"))
            assertFalse(logs.contains(transaction.authCode))
            assertFalse(logs.contains(transaction.nsu))
            assertFalse(logs.contains(transaction.orderId))
        }

    @Test
    fun `AC-09 - dado falha de armazenamento quando processar entao propaga a falha e nao encerra a sessao`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(aPurchase()))
            repository.failStorage = true

            val result = useCase(repository)(approved())

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
            assertEquals(0, gateway.finished)
        }

    private class RecordingLogger : Logger {
        val infos = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val all: List<String> get() = infos + warnings

        override fun info(message: String) {
            infos += message
        }

        override fun warn(
            message: String,
            throwable: Throwable?,
        ) {
            warnings += message
        }

        override fun error(
            message: String,
            throwable: Throwable?,
        ) {
            warnings += message
        }
    }
}
