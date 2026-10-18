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

import android.database.sqlite.SQLiteException
import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.database.BilheteriaDatabase
import com.rafaeloliveira.bilheteria.core.database.EventQuantity
import com.rafaeloliveira.bilheteria.core.database.PurchaseDao
import com.rafaeloliveira.bilheteria.core.database.PurchaseEntity
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aTransaction
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

/** Integration tests of [DefaultPurchaseRepository] on a real in-memory Room database (plan §10). */
@RunWith(RobolectricTestRunner::class)
class DefaultPurchaseRepositoryTest {
    private val logger = RecordingLogger()
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val databaseRule = InMemoryDatabaseRule(dispatcher)

    private fun newDatabase(): BilheteriaDatabase = databaseRule.newDatabase()

    private fun repository(dao: PurchaseDao): DefaultPurchaseRepository =
        DefaultPurchaseRepository(dao, dispatcher, logger)

    private fun purchase(
        reference: String,
        status: PurchaseStatus,
        eventId: String = "e1",
        quantity: Int = 1,
        minutesAfterNow: Long = 0,
    ): Purchase =
        aPurchase(
            reference = reference,
            event = anEvent(id = eventId),
            quantity = quantity,
            status = status,
            createdAt = TEST_NOW.plus(Duration.ofMinutes(minutesAfterNow)),
        )

    @Test
    fun `AC-08 - dado compra aberta quando criar outra pendente entao rejeita com PurchaseAlreadyOpen`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            val first = purchase("p1", PurchaseStatus.Pending)

            val created = repository.createPending(first)
            val second = repository.createPending(purchase("p2", PurchaseStatus.Pending))

            assertEquals(DataResult.Success(Unit), created)
            assertEquals(DataResult.Failure(TicketingError.PurchaseAlreadyOpen), second)
            assertEquals(DataResult.Success<Purchase?>(first), repository.findOpen())
        }

    @Test
    fun `AC-08 - dado compra em recuperacao quando criar outra pendente entao rejeita com PurchaseAlreadyOpen`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            repository.createPending(purchase("p1", PurchaseStatus.Pending))
            repository.update(purchase("p1", PurchaseStatus.Recovering))

            val second = repository.createPending(purchase("p2", PurchaseStatus.Pending))

            assertEquals(DataResult.Failure(TicketingError.PurchaseAlreadyOpen), second)
        }

    @Test
    fun `AC-08 - dado duas tentativas simultaneas quando criar pendente entao apenas uma vence`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())

            val results =
                listOf("p1", "p2", "p3", "p4")
                    .map { ref -> async { repository.createPending(purchase(ref, PurchaseStatus.Pending)) } }
                    .awaitAll()

            assertEquals(1, results.count { it is DataResult.Success })
            assertEquals(3, results.count { it == DataResult.Failure(TicketingError.PurchaseAlreadyOpen) })
        }

    @Test
    fun `AC-08 - dado compra encerrada quando criar nova pendente entao permite`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            repository.createPending(purchase("p1", PurchaseStatus.Pending))
            repository.update(purchase("p1", PurchaseStatus.Cancelled))

            val second = repository.createPending(purchase("p2", PurchaseStatus.Pending, minutesAfterNow = 1))

            assertEquals(DataResult.Success(Unit), second)
        }

    @Test
    fun `AC-08 - dado compra aberta quando observar a compra aberta entao emite e depois nulo ao encerrar`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            val pending = purchase("p1", PurchaseStatus.Pending)
            repository.createPending(pending)

            repository.observeOpenPurchase().test {
                assertEquals(DataResult.Success<Purchase?>(pending), awaitItem())
                repository.update(purchase("p1", PurchaseStatus.Failed).copy(failureReason = FailureReason.Generic))
                assertEquals(DataResult.Success<Purchase?>(null), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-16 - dado compra aprovada quando atualizar com a transacao entao a leitura devolve o comprovante`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            repository.createPending(purchase("p1", PurchaseStatus.Pending))
            val approved = purchase("p1", PurchaseStatus.Approved).copy(transaction = aTransaction())

            val updated = repository.update(approved)

            assertEquals(DataResult.Success(Unit), updated)
            repository.observePurchase("p1").test {
                assertEquals(DataResult.Success(approved), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-17 - dado aprovadas e outros status quando observar entao soma so as aprovadas por evento`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            repository.createPending(purchase("p1", PurchaseStatus.Approved, eventId = "e1", quantity = 2))
            repository.createPending(purchase("p2", PurchaseStatus.Approved, eventId = "e1", quantity = 3))
            repository.createPending(purchase("p3", PurchaseStatus.Approved, eventId = "e2", quantity = 4))
            repository.createPending(purchase("p4", PurchaseStatus.Declined, eventId = "e1", quantity = 5))
            repository.createPending(purchase("p5", PurchaseStatus.Cancelled, eventId = "e2", quantity = 6))
            repository.createPending(purchase("p6", PurchaseStatus.Failed, eventId = "e1", quantity = 7))
            repository.createPending(purchase("p7", PurchaseStatus.Pending, eventId = "e1", quantity = 8))

            repository.observeApprovedQuantities().test {
                assertEquals(DataResult.Success(mapOf("e1" to 5, "e2" to 4)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-17 - dado compra pendente quando for aprovada entao a disponibilidade observada reflete a aprovacao`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            repository.createPending(purchase("p1", PurchaseStatus.Pending, eventId = "e1", quantity = 1))

            repository.observeApprovedQuantities().test {
                assertEquals(DataResult.Success(emptyMap<String, Int>()), awaitItem())
                repository.update(purchase("p1", PurchaseStatus.Approved, eventId = "e1", quantity = 1))
                assertEquals(DataResult.Success(mapOf("e1" to 1)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-18 - dado compras de varios status quando observar historico entao so vendas em ordem decrescente`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            val oldest = purchase("p1", PurchaseStatus.Approved, minutesAfterNow = 0)
            val middle = purchase("p2", PurchaseStatus.Declined, minutesAfterNow = 10)
            val newest = purchase("p3", PurchaseStatus.Cancelled, minutesAfterNow = 20)
            listOf(
                oldest,
                purchase("p4", PurchaseStatus.Failed, minutesAfterNow = 5),
                newest,
                middle,
                purchase("p5", PurchaseStatus.Recovering, minutesAfterNow = 30),
            ).forEach { repository.createPending(it) }

            repository.observeHistory().test {
                assertEquals(DataResult.Success(listOf(newest, middle, oldest)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-20 - dado nenhuma compra quando observar historico entao emite lista vazia`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())

            repository.observeHistory().test {
                assertEquals(DataResult.Success(emptyList<Purchase>()), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-21 - dado falha do banco ao ler quando observar historico entao emite StorageFailure`() =
        runTest(dispatcher) {
            val repository = repository(FailingPurchaseDao())

            repository.observeHistory().test {
                assertEquals(DataResult.Failure(TicketingError.StorageFailure), awaitItem())
                awaitComplete()
            }
            assertTrue(logger.errors.single().startsWith("purchase_storage_failure(operation=observe_history)"))
        }

    @Test
    fun `AC-21 - dado falha do banco ao ler quando observar aprovadas ou a compra aberta entao emite StorageFailure`() =
        runTest(dispatcher) {
            val repository = repository(FailingPurchaseDao())

            repository.observeApprovedQuantities().test {
                assertEquals(DataResult.Failure(TicketingError.StorageFailure), awaitItem())
                awaitComplete()
            }
            repository.observeOpenPurchase().test {
                assertEquals(DataResult.Failure(TicketingError.StorageFailure), awaitItem())
                awaitComplete()
            }
            repository.observePurchase("p1").test {
                assertEquals(DataResult.Failure(TicketingError.StorageFailure), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `AC-07 - dado falha do banco ao gravar quando criar pendente entao devolve StorageFailure`() =
        runTest(dispatcher) {
            val repository = repository(FailingPurchaseDao())

            val result = repository.createPending(purchase("p1", PurchaseStatus.Pending))

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
        }

    @Test
    fun `AC-15 - dado falha do banco quando atualizar ou buscar a compra aberta entao devolve StorageFailure`() =
        runTest(dispatcher) {
            val repository = repository(FailingPurchaseDao())

            assertEquals(
                DataResult.Failure(TicketingError.StorageFailure),
                repository.update(purchase("p1", PurchaseStatus.Approved)),
            )
            assertEquals(DataResult.Failure(TicketingError.StorageFailure), repository.findOpen())
        }

    @Test
    fun `AC-07 - dado referencia ja gravada quando criar pendente de novo entao devolve StorageFailure`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            repository.createPending(purchase("p1", PurchaseStatus.Approved))

            val result = repository.createPending(purchase("p1", PurchaseStatus.Pending))

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
        }

    @Test
    fun `AC-28 - dado compra que falhou quando buscar por id entao devolve a compra e nulo se nao existir`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())
            val failed = purchase("p1", PurchaseStatus.Failed)
            repository.createPending(purchase("p1", PurchaseStatus.Pending))
            repository.update(failed)

            assertEquals(DataResult.Success<Purchase?>(failed), repository.findById("p1"))
            assertEquals(DataResult.Success<Purchase?>(null), repository.findById("unknown"))
        }

    @Test
    fun `AC-28 - dado falha do banco quando buscar por id entao devolve StorageFailure`() =
        runTest(dispatcher) {
            val repository = repository(FailingPurchaseDao())

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), repository.findById("p1"))
        }

    @Test
    fun `AC-15 - dado compra inexistente quando atualizar entao devolve StorageFailure`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())

            val result = repository.update(purchase("ghost", PurchaseStatus.Approved))

            assertEquals(DataResult.Failure(TicketingError.StorageFailure), result)
        }

    @Test
    fun `AC-19 - dado linha com status desconhecido quando observar a compra por id entao emite StorageFailure`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val dao = database.purchaseDao()
            dao.insertIfNoneOpen(corruptedRow(status = "Bogus"), openStatuses = emptyList())
            val repository = repository(dao)

            repository.observePurchase("corrupted").test {
                assertEquals(DataResult.Failure(TicketingError.StorageFailure), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `AC-18 - dado compra inexistente quando observar por id entao emite StorageFailure`() =
        runTest(dispatcher) {
            val database = newDatabase()
            val repository = repository(database.purchaseDao())

            repository.observePurchase("ghost").test {
                assertEquals(DataResult.Failure(TicketingError.StorageFailure), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun corruptedRow(status: String) =
        PurchaseEntity(
            id = "corrupted",
            eventId = "e1",
            eventName = "Evento",
            eventStartsAt = 0,
            venue = "Local",
            unitPriceCents = 100,
            quantity = 1,
            totalCents = 100,
            status = status,
            failureReason = null,
            cieloOrderId = null,
            authCode = null,
            nsu = null,
            brand = null,
            createdAt = 0,
            updatedAt = 0,
        )
}

/** A DAO whose every operation fails like a broken database would. */
private class FailingPurchaseDao : PurchaseDao() {
    override fun observeById(id: String): Flow<PurchaseEntity?> = failing()

    override fun observeByStatuses(statuses: List<String>): Flow<List<PurchaseEntity>> = failing()

    override fun observeOpen(openStatuses: List<String>): Flow<PurchaseEntity?> = failing()

    override suspend fun findOpen(openStatuses: List<String>): PurchaseEntity? = throw SQLiteException("disk I/O error")

    override suspend fun findById(id: String): PurchaseEntity? = throw SQLiteException("disk I/O error")

    override fun observeQuantitiesByStatus(status: String): Flow<List<EventQuantity>> = failing()

    override suspend fun insert(entity: PurchaseEntity): Unit = throw SQLiteException("disk I/O error")

    override suspend fun update(entity: PurchaseEntity): Int = throw SQLiteException("disk I/O error")

    private fun <T> failing(): Flow<T> = flow { throw SQLiteException("disk I/O error") }
}
