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

import com.rafaeloliveira.bilheteria.core.common.AppError
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FixedReferenceGenerator
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.ZoneOffset

class StartPurchaseUseCaseTest {
    private val reference = "91ccb5d9-ccf6-4471-bfe2-413d5bc48637"
    private val clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)
    private val event = AvailableEvent(anEvent(unitPriceCents = 5_000), available = 10)
    private val repository = FakePurchaseRepository()
    private val gateway = FakePaymentGateway()

    private fun useCase(paymentGateway: PaymentGateway = gateway) =
        StartPurchaseUseCase(repository, paymentGateway, FixedReferenceGenerator(reference, "second-ref"), clock)

    @Test
    fun `AC-07 - dado compra confirmada quando pagar entao grava pendente antes de abrir a Cielo`() =
        runTest {
            val seenAtRequest = mutableListOf<List<Purchase>>()
            val spy =
                object : PaymentGateway {
                    override suspend fun requestPayment(purchase: Purchase): DataResult<Unit> {
                        seenAtRequest += repository.all
                        return gateway.requestPayment(purchase)
                    }

                    override suspend fun requestLookup(page: Int): DataResult<Unit> = gateway.requestLookup(page)

                    override fun finish() = gateway.finish()
                }

            useCase(spy)(event, quantity = 3)

            assertEquals(1, seenAtRequest.size)
            val stored = seenAtRequest.single().single()
            assertEquals(PurchaseStatus.Pending, stored.status)
            assertEquals(reference, stored.reference.value)
            assertEquals(listOf(stored), gateway.paymentRequests)
        }

    @Test
    fun `AC-07 - dado tres ingressos quando pagar entao devolve compra pendente com total referencia e snapshot`() =
        runTest {
            val result = useCase()(event, quantity = 3)

            val purchase = (result as DataResult.Success).value
            assertEquals(PurchaseReference(reference), purchase.reference)
            assertEquals(reference, purchase.id)
            assertEquals(PurchaseStatus.Pending, purchase.status)
            assertEquals(Money(15_000), purchase.total)
            assertEquals(3, purchase.quantity)
            assertEquals(Money(5_000), purchase.unitPrice)
            assertEquals(event.event.id, purchase.eventId)
            assertEquals(event.event.name, purchase.eventName)
            assertEquals(event.event.startsAt, purchase.eventStartsAt)
            assertEquals(event.event.venue, purchase.venue)
            assertEquals(TEST_NOW, purchase.createdAt)
            assertEquals(TEST_NOW, purchase.updatedAt)
            assertEquals(listOf(purchase), repository.all)
        }

    @Test
    fun `AC-07 - dado quantidade fora do estoque quando pagar entao rejeita sem gravar nem abrir a Cielo`() =
        runTest {
            assertThrows<IllegalArgumentException> { useCase()(event, quantity = 0) }
            assertThrows<IllegalArgumentException> { useCase()(event, quantity = 11) }

            assertTrue(repository.all.isEmpty())
            assertTrue(gateway.paymentRequests.isEmpty())
        }

    @Test
    fun `AC-06 - dado 50 disponiveis quando pagar 11 ingressos entao rejeita pelo teto de 10 por compra`() =
        runTest {
            val plenty = AvailableEvent(anEvent(unitPriceCents = 5_000), available = 50)

            assertThrows<IllegalArgumentException> { useCase()(plenty, quantity = 11) }
            assertTrue(useCase()(plenty, quantity = 10) is DataResult.Success)
            assertTrue(repository.all.single().quantity == 10)
        }

    @Test
    fun `AC-08 - dado compra em andamento quando pagar de novo entao nao abre segundo pagamento`() =
        runTest {
            val useCase = useCase()
            val first = useCase(event, quantity = 1) as DataResult.Success

            val second = useCase(event, quantity = 2)

            assertEquals(DataResult.Failure(TicketingError.PurchaseAlreadyOpen), second)
            assertEquals(listOf(first.value), gateway.paymentRequests)
            assertEquals(listOf(first.value), repository.all)
        }

    @Test
    fun `AC-08 - dado compra aberta preexistente quando pagar entao recusa e nao chama o gateway`() =
        runTest {
            val open = aPurchase(reference = "open-ref", status = PurchaseStatus.Recovering)
            val repository = FakePurchaseRepository(listOf(open))
            val useCase = StartPurchaseUseCase(repository, gateway, FixedReferenceGenerator(reference), clock)

            val result = useCase(event, quantity = 1)

            assertEquals(DataResult.Failure(TicketingError.PurchaseAlreadyOpen), result)
            assertTrue(gateway.paymentRequests.isEmpty())
            assertEquals(listOf(open), repository.all)
        }

    @Test
    fun `AC-12 - dado app Cielo ausente quando pagar entao erro sem cobranca e compra falhou`() =
        runTest {
            gateway.terminalAvailable = false

            val result = useCase()(event, quantity = 2)

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertTrue(gateway.paymentRequests.isEmpty())
            val stored = repository.all.single()
            assertEquals(PurchaseStatus.Failed, stored.status)
            assertEquals(FailureReason.TerminalAppUnavailable, stored.failureReason)
            assertEquals(reference, stored.id)
            assertTrue(repository.findOpen().let { it is DataResult.Success && it.value == null })
        }

    @Test
    fun `AC-12 - dado app Cielo ausente quando pagar de novo apos falha entao permite nova tentativa`() =
        runTest {
            val useCase = useCase()
            gateway.terminalAvailable = false
            useCase(event, quantity = 1)
            gateway.terminalAvailable = true

            val retry = useCase(event, quantity = 1)

            assertTrue(retry is DataResult.Success)
            assertEquals(PurchaseStatus.Pending, (retry as DataResult.Success).value.status)
        }

    @Test
    fun `AC-07 - dado falha de armazenamento quando pagar entao devolve a falha e nao chama o gateway`() =
        runTest {
            repository.failStorage = true

            val result = useCase()(event, quantity = 1)

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
            assertTrue(gateway.paymentRequests.isEmpty())
            assertTrue(repository.all.isEmpty())
        }

    @Test
    fun `AC-07 - dado falha generica do gateway quando pagar entao compra falhou com motivo generico`() =
        runTest {
            val failing =
                object : PaymentGateway {
                    override suspend fun requestPayment(purchase: Purchase): DataResult<Unit> =
                        DataResult.Failure(AppError.Unknown)

                    override suspend fun requestLookup(page: Int): DataResult<Unit> = DataResult.Success(Unit)

                    override fun finish() = Unit
                }

            val result = useCase(failing)(event, quantity = 1)

            assertEquals(DataResult.Failure(AppError.Unknown), result)
            val stored = repository.all.single()
            assertEquals(PurchaseStatus.Failed, stored.status)
            assertEquals(FailureReason.Generic, stored.failureReason)
        }
}
