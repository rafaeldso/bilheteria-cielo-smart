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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase

import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.database.PurchaseDao
import com.rafaeloliveira.bilheteria.core.database.PurchaseEntity
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the mapper writes to Room must come back identical (plan §5), including nullable fields. */
@RunWith(RobolectricTestRunner::class)
class PurchaseMapperRoundTripTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val databaseRule = InMemoryDatabaseRule(dispatcher)

    private fun repository(): Pair<DefaultPurchaseRepository, PurchaseDao> {
        val dao = databaseRule.newDatabase().purchaseDao()
        return DefaultPurchaseRepository(dao, dispatcher, RecordingLogger()) to dao
    }

    @Test
    fun `AC-13 - dado compra Failed com cada motivo quando gravar e ler de volta entao o motivo e preservado`() =
        runTest(dispatcher) {
            FailureReason.entries.forEach { reason ->
                val failed =
                    aPurchase(reference = "failed-${reason.name}", status = PurchaseStatus.Failed)
                        .copy(failureReason = reason)
                val (repository, _) = repository()
                repository.createPending(failed)

                repository.observePurchase(failed.id).test {
                    assertEquals(DataResult.Success(failed), awaitItem(), "reason $reason")
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    @Test
    fun `AC-15 - dado compra em recuperacao com cada motivo quando buscar a aberta entao o motivo e preservado`() =
        runTest(dispatcher) {
            FailureReason.entries.forEach { reason ->
                val recovering =
                    aPurchase(reference = "recovering-${reason.name}", status = PurchaseStatus.Recovering)
                        .copy(failureReason = reason)
                val (repository, _) = repository()
                repository.createPending(recovering)

                assertEquals(DataResult.Success<Purchase?>(recovering), repository.findOpen(), "reason $reason")
            }
        }

    @Test
    fun `AC-16 - dado transacao sem bandeira quando gravar e ler de volta entao a bandeira continua nula`() =
        runTest(dispatcher) {
            val (repository, _) = repository()
            val approved =
                aPurchase(status = PurchaseStatus.Approved)
                    .copy(transaction = aTransaction().copy(brand = null))
            repository.createPending(approved)

            repository.observePurchase(approved.id).test {
                val read = (awaitItem() as DataResult.Success).value
                assertEquals(approved, read)
                assertEquals(null, read.transaction?.brand)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-16 - dado compra sem transacao nem motivo quando gravar e ler de volta entao ambos continuam nulos`() =
        runTest(dispatcher) {
            val (repository, _) = repository()
            val cancelled = aPurchase(status = PurchaseStatus.Cancelled)
            repository.createPending(cancelled)

            repository.observePurchase(cancelled.id).test {
                val read = (awaitItem() as DataResult.Success).value
                assertEquals(null, read.transaction)
                assertEquals(null, read.failureReason)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Expected behavior: the receipt is all-or-nothing. A row with only some of orderId / authCode / nsu is
     * not a valid [TransactionData], so it is read as "no transaction" instead of a receipt with blanks.
     * The mapper never writes such a row (the domain type requires the three), so this only guards corruption.
     */
    @Test
    fun `AC-16 - dado linha com transacao incompleta quando ler entao devolve compra sem comprovante`() =
        runTest(dispatcher) {
            val (repository, dao) = repository()
            val complete = aTransaction()
            val partialRows =
                listOf(
                    entity("p-no-order", orderId = null, authCode = complete.authCode, nsu = complete.nsu),
                    entity("p-no-auth", orderId = complete.orderId, authCode = null, nsu = complete.nsu),
                    entity("p-no-nsu", orderId = complete.orderId, authCode = complete.authCode, nsu = null),
                )
            partialRows.forEach { dao.insertIfNoneOpen(it, openStatuses = emptyList()) }

            partialRows.forEach { row ->
                repository.observePurchase(row.id).test {
                    val read = (awaitItem() as DataResult.Success).value
                    assertEquals(null, read.transaction, row.id)
                    assertEquals(PurchaseStatus.Approved, read.status, row.id)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    private fun entity(
        id: String,
        orderId: String?,
        authCode: String?,
        nsu: String?,
    ) = PurchaseEntity(
        id = id,
        eventId = "e1",
        eventName = "Evento",
        eventStartsAt = 0,
        venue = "Local",
        unitPriceCents = 100,
        quantity = 1,
        totalCents = 100,
        status = PurchaseStatus.Approved.name,
        failureReason = null,
        cieloOrderId = orderId,
        authCode = authCode,
        nsu = nsu,
        brand = "Visa",
        createdAt = 0,
        updatedAt = 0,
    )
}
