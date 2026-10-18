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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Event
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import java.time.Instant

/** Fixed "now" for tests (Constitution Art. IV.3: no real clock). */
val TEST_NOW: Instant = Instant.parse("2026-10-17T12:00:00Z")

/** Fictitious test data only (testing.md §7). */
fun anEvent(
    id: String = "e1",
    name: String = "Noite de Jazz no Parque",
    startsAt: Instant = Instant.parse("2026-11-15T23:00:00Z"),
    venue: String = "Concha Acústica Municipal",
    unitPriceCents: Long = 5_000,
    totalTickets: Int = 50,
) = Event(id, name, startsAt, venue, Money(unitPriceCents), totalTickets)

fun aPurchase(
    reference: String = "91ccb5d9-ccf6-4471-bfe2-413d5bc48637",
    event: Event = anEvent(),
    quantity: Int = 2,
    status: PurchaseStatus = PurchaseStatus.Pending,
    failureReason: FailureReason? = null,
    transaction: TransactionData? = null,
    createdAt: Instant = TEST_NOW,
) = Purchase(
    id = reference,
    reference = PurchaseReference(reference),
    eventId = event.id,
    eventName = event.name,
    eventStartsAt = event.startsAt,
    venue = event.venue,
    unitPrice = event.unitPrice,
    quantity = quantity,
    total = event.unitPrice * quantity,
    status = status,
    failureReason = failureReason,
    transaction = transaction,
    createdAt = createdAt,
    updatedAt = createdAt,
)

fun aTransaction() = TransactionData(orderId = "order-1", authCode = "A1B2C3", nsu = "000123456", brand = "Visa")
