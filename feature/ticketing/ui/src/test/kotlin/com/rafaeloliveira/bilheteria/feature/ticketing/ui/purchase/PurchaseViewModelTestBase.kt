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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.core.testing.MainDispatcherExtension
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeEventCatalogRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePaymentGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakePurchaseRepository
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FixedReferenceGenerator
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.HandlePaymentOutcomeUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveAvailableEventsUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObserveOpenPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.ObservePurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.PurchaseWriteLock
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.RecoverPendingPurchaseUseCase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.StartPurchaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Clock
import java.time.ZoneOffset

internal const val UNIT_PRICE_CENTS = 5_000L
internal const val THREE_TICKETS_CENTS = 15_000L
internal const val TOTAL_TICKETS = 50
internal const val FIRST_REFERENCE = "91ccb5d9-ccf6-4471-bfe2-413d5bc48637"
internal const val SECOND_REFERENCE = "0b5d3c2a-6f1e-4a67-9c1b-7f3d2e8a4b10"

internal abstract class PurchaseViewModelTestBase {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    protected val clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)
    private val writeLock = PurchaseWriteLock()
    protected val event = anEvent(unitPriceCents = UNIT_PRICE_CENTS, totalTickets = TOTAL_TICKETS)
    protected val catalog = FakeEventCatalogRepository(listOf(event))
    protected val gateway = FakePaymentGateway()
    protected val references = FixedReferenceGenerator(FIRST_REFERENCE, SECOND_REFERENCE)

    protected fun repositoryWith(vararg purchases: Purchase) = FakePurchaseRepository(purchases.toList())

    protected fun createViewModel(
        repository: PurchaseRepository,
        handle: SavedStateHandle = SavedStateHandle(mapOf("eventId" to event.id)),
        paymentGateway: PaymentGateway = gateway,
    ) = PurchaseViewModel(
        observeAvailableEvents = ObserveAvailableEventsUseCase(catalog, repository, clock),
        observeOpenPurchase = ObserveOpenPurchaseUseCase(repository),
        observePurchase = ObservePurchaseUseCase(repository),
        startPurchase = StartPurchaseUseCase(repository, paymentGateway, references, clock),
        recoverPending = RecoverPendingPurchaseUseCase(repository, paymentGateway, clock, writeLock),
        savedStateHandle = handle,
    )

    protected fun outcomes(repository: PurchaseRepository) =
        HandlePaymentOutcomeUseCase(
            repository = repository,
            gateway = gateway,
            recover = RecoverPendingPurchaseUseCase(repository, gateway, clock, writeLock),
            clock = clock,
            logger = SilentLogger,
            lock = writeLock,
        )

    /** Approved sales that leave [remaining] tickets of the event. */
    protected fun soldOut(remaining: Int) =
        aPurchase(
            reference = "sold",
            event = event,
            quantity = TOTAL_TICKETS - remaining,
            status = PurchaseStatus.Approved,
        )

    protected fun summary(available: Int? = TOTAL_TICKETS) =
        EventSummary(event.id, event.name, event.startsAt, event.venue, event.unitPrice, available)

    /** Keeps the state subscribed (the state is shared while subscribed) and discards what Turbine buffered. */
    protected suspend fun observing(
        viewModel: PurchaseViewModel,
        block: suspend () -> Unit,
    ) {
        viewModel.uiState.test {
            block()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** BR-10, BR-27: whatever the screen shows, "Pagar" opens no payment. */
    protected fun TestScope.assertPayOpensNoPayment(viewModel: PurchaseViewModel) {
        viewModel.onEvent(PurchaseUiEvent.OnPay)
        advanceUntilIdle()
        assertTrue(gateway.paymentRequests.isEmpty())
    }

    /** Runs everything pending on the test dispatcher and returns what the (subscribed) state holds. */
    protected fun TestScope.latest(viewModel: PurchaseViewModel): PurchaseUiState {
        advanceUntilIdle()
        return viewModel.uiState.value
    }
}

/**
 * Repository whose open purchase cannot be read until [readable] is set, and whose single-purchase record cannot be
 * read until [recordReadable] is set: the real flow emits the failure and ends (BR-26). Every other operation goes to
 * the in-memory [delegate].
 */
internal class UnreadableOpenPurchaseRepository(
    private val delegate: FakePurchaseRepository,
) : PurchaseRepository by delegate {
    var readable: Boolean = false
    var recordReadable: Boolean = true

    override fun observePurchase(id: String): Flow<DataResult<Purchase>> =
        if (recordReadable) {
            delegate.observePurchase(id)
        } else {
            flowOf(DataResult.Failure(TicketingError.StorageFailure))
        }

    override fun observeOpenPurchase(): Flow<DataResult<Purchase?>> =
        if (readable) {
            delegate.observeOpenPurchase()
        } else {
            flowOf(DataResult.Failure(TicketingError.StorageFailure))
        }
}

private object SilentLogger : Logger {
    override fun info(message: String) = Unit

    override fun warn(
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun error(
        message: String,
        throwable: Throwable?,
    ) = Unit
}
