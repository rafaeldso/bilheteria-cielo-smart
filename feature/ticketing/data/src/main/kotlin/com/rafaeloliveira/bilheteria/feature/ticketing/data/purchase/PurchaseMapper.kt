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

import com.rafaeloliveira.bilheteria.core.database.PurchaseEntity
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import java.time.Instant

internal fun Purchase.toEntity(): PurchaseEntity =
    PurchaseEntity(
        id = id,
        eventId = eventId,
        eventName = eventName,
        eventStartsAt = eventStartsAt.toEpochMilli(),
        venue = venue,
        unitPriceCents = unitPrice.cents,
        quantity = quantity,
        totalCents = total.cents,
        status = status.name,
        failureReason = failureReason?.name,
        cieloOrderId = transaction?.orderId,
        authCode = transaction?.authCode,
        nsu = transaction?.nsu,
        brand = transaction?.brand,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

/**
 * Rebuilds the domain purchase. Throws [IllegalArgumentException] for a row the domain would not accept
 * (unknown status, negative amount); the repository maps it to `StorageFailure`.
 */
internal fun PurchaseEntity.toDomain(): Purchase =
    Purchase(
        id = id,
        reference = PurchaseReference(id),
        eventId = eventId,
        eventName = eventName,
        eventStartsAt = Instant.ofEpochMilli(eventStartsAt),
        venue = venue,
        unitPrice = Money(unitPriceCents),
        quantity = quantity,
        total = Money(totalCents),
        status = PurchaseStatus.valueOf(status),
        failureReason = failureReason?.let { FailureReason.valueOf(it) },
        transaction = toTransaction(),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

private fun PurchaseEntity.toTransaction(): TransactionData? {
    val orderId = cieloOrderId
    val code = authCode
    val receiptNsu = nsu
    return if (orderId != null && code != null && receiptNsu != null) {
        TransactionData(orderId = orderId, authCode = code, nsu = receiptNsu, brand = brand)
    } else {
        null
    }
}
