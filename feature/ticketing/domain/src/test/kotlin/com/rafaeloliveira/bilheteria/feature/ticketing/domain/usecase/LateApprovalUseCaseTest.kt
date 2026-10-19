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
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.ZoneOffset

/**
 * Late approval (BR-29, AC-28) of [HandlePaymentOutcomeUseCase]: a purchase that failed as "not found" and was
 * paid later.
 */
class LateApprovalUseCaseTest {
    private val now = TEST_NOW.plusSeconds(30)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val writeLock = PurchaseWriteLock()
    private val gateway = FakePaymentGateway()
    private val logger = RecordingLogger()
    private val total = Money(10_000)

    private fun useCase(repository: FakePurchaseRepository) =
        HandlePaymentOutcomeUseCase(
            repository = repository,
            gateway = gateway,
            recover = RecoverPendingPurchaseUseCase(repository, gateway, clock, writeLock),
            clock = clock,
            logger = logger,
            lock = writeLock,
        )

    private fun approved(
        reference: PurchaseReference,
        amount: Money = total,
    ) = PaymentOutcome.Approved(reference, amount, aTransaction())

    private val lateReference = PurchaseReference("late-reference")

    private fun notFoundFailure(reason: FailureReason = FailureReason.NotFound) =
        aPurchase(reference = lateReference.value, status = PurchaseStatus.Failed, failureReason = reason)

    @Test
    fun `AC-28 - dado compra falhou nao encontrada e nenhuma aberta quando chega aprovado entao registra aprovada`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(notFoundFailure()))

            val result = useCase(repository)(approved(reference = lateReference))

            val expected =
                aPurchase(
                    reference = lateReference.value,
                    status = PurchaseStatus.Approved,
                    transaction = aTransaction(),
                ).copy(updatedAt = now)
            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(expected), repository.all)
            assertEquals(listOf("late_approval reference=${lateReference.value}"), logger.infos)
            assertEquals(0, gateway.finished)
        }

    @Test
    fun `AC-28 - dado compra aberta quando chega aprovado tardio de outra compra entao nao altera a aberta`() =
        runTest {
            val open = aPurchase()
            val repository = FakePurchaseRepository(listOf(notFoundFailure(), open))

            useCase(repository)(approved(reference = lateReference))

            assertEquals(PurchaseStatus.Approved, repository.all.first { it.id == lateReference.value }.status)
            assertEquals(open, repository.all.first { it.id == open.id })
            assertEquals(0, gateway.finished)
        }

    @ParameterizedTest
    @ValueSource(longs = [100, 9_999, 10_001, 20_000])
    fun `AC-28 - dado valor pago diferente do total quando chega aprovado tardio entao descarta`(cents: Long) =
        runTest {
            val failure = notFoundFailure()
            val repository = FakePurchaseRepository(listOf(failure))

            useCase(repository)(approved(reference = lateReference, amount = Money(cents)))

            assertEquals(listOf(failure), repository.all)
            assertEquals(listOf("callback_discarded reason=no_open_purchase"), logger.warnings)
        }

    @Test
    fun `AC-28 - dado reference desconhecido quando chega aprovado entao descarta e nao cria compra`() =
        runTest {
            val failure = notFoundFailure()
            val repository = FakePurchaseRepository(listOf(failure))

            val result = useCase(repository)(approved(reference = PurchaseReference("unknown-reference")))

            assertEquals(DataResult.Success(Unit), result)
            assertEquals(listOf(failure), repository.all)
            assertEquals(listOf("callback_discarded reason=no_open_purchase"), logger.warnings)
            assertEquals(emptyList<String>(), logger.infos)
        }

    @ParameterizedTest
    @EnumSource(PurchaseStatus::class, names = ["Approved", "Declined", "Cancelled"])
    fun `AC-28 - dado compra que nao esta falha quando chega aprovado com o mesmo reference entao nao altera`(
        status: PurchaseStatus,
    ) = runTest {
        // failureReason is NotFound on purpose: only the status must keep the purchase out of the late approval.
        val purchase = notFoundFailure().copy(status = status)
        val repository = FakePurchaseRepository(listOf(purchase))

        useCase(repository)(approved(reference = lateReference))

        assertEquals(listOf(purchase), repository.all)
        assertEquals(listOf("callback_discarded reason=no_open_purchase"), logger.warnings)
        assertEquals(emptyList<String>(), logger.infos)
    }

    @Test
    fun `AC-28 - dado aprovacao tardia registrada quando chega outro callback igual entao nao reescreve`() =
        runTest {
            val repository = FakePurchaseRepository(listOf(notFoundFailure()))
            val useCase = useCase(repository)
            useCase(approved(reference = lateReference))
            val afterFirst = repository.all

            useCase(PaymentOutcome.Approved(lateReference, total, aTransaction().copy(authCode = "999999")))

            assertEquals(afterFirst, repository.all)
            assertEquals(1, logger.infos.size)
        }

    @Test
    fun `AC-28 - dado falha de armazenamento quando chega aprovado tardio entao devolve a falha e nao registra`() =
        runTest {
            val failure = notFoundFailure()
            val repository = FakePurchaseRepository(listOf(failure)).apply { failStorage = true }

            val result = useCase(repository)(approved(reference = lateReference))

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
            assertEquals(listOf(failure), repository.all)
            assertEquals(emptyList<String>(), logger.infos)
        }

    @ParameterizedTest
    @EnumSource(FailureReason::class, names = ["NotFound"], mode = EnumSource.Mode.EXCLUDE)
    fun `AC-28 - dado compra que falhou por outro motivo quando chega aprovado entao descarta`(reason: FailureReason) =
        runTest {
            val failure = notFoundFailure(reason)
            val repository = FakePurchaseRepository(listOf(failure))

            useCase(repository)(approved(reference = lateReference))

            assertEquals(listOf(failure), repository.all)
            assertEquals(listOf("callback_discarded reason=no_open_purchase"), logger.warnings)
        }

    @Test
    fun `AC-28 - dado cancelamento com reference de compra falhou quando chega entao nao reabre a compra`() =
        runTest {
            val failure = notFoundFailure()
            val repository = FakePurchaseRepository(listOf(failure))

            useCase(repository)(PaymentOutcome.Cancelled(lateReference))

            assertEquals(listOf(failure), repository.all)
        }

    private class RecordingLogger : Logger {
        val infos = mutableListOf<String>()
        val warnings = mutableListOf<String>()

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
