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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.model

import java.time.Instant

/**
 * What a ticket QR Code encodes (BR-31): `bilheteria:1:<reference>:<ordinal>`. Only the purchase `reference` and the
 * ticket order, never card data, transaction data, credentials or PII. [toString] is masked so the content never ends
 * up in a log or a crash report (security.md §2).
 */
@JvmInline
value class TicketCode(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Ticket code cannot be blank" }
    }

    override fun toString(): String = "TicketCode(***)"
}

/** One ticket of an approved purchase: [ordinal] of [total] (BR-30, BR-35). */
data class Ticket(
    val ordinal: Int,
    val total: Int,
    val code: TicketCode,
) {
    init {
        require(total >= 1) { "A purchase has at least one ticket" }
        require(ordinal in 1..total) { "Ticket order must be between 1 and the total" }
    }
}

/** The tickets of one approved purchase, with the event data printed next to each QR Code (BR-35). */
data class TicketSet(
    val purchaseId: String,
    val eventName: String,
    val eventStartsAt: Instant,
    val venue: String,
    val tickets: List<Ticket>,
)

/** Square QR Code matrix in plain Kotlin (the domain cannot depend on the encoding library). `true` = dark module. */
class QrMatrix(
    val side: Int,
    private val modules: BooleanArray,
) {
    init {
        require(side > 0) { "QR matrix side must be positive" }
        require(modules.size == side * side) { "QR matrix must have side * side modules" }
    }

    fun isDark(
        x: Int,
        y: Int,
    ): Boolean {
        require(x in 0 until side && y in 0 until side) { "Module outside the matrix" }
        return modules[y * side + x]
    }

    override fun equals(other: Any?): Boolean =
        other is QrMatrix && side == other.side && modules.contentEquals(other.modules)

    override fun hashCode(): Int = 31 * side + modules.contentHashCode()

    override fun toString(): String = "QrMatrix(side=$side)"
}

/** A [Ticket] with its encoded [qr] (BR-30). */
data class TicketWithQr(
    val ticket: Ticket,
    val qr: QrMatrix,
)

/** What the tickets screen shows: the event data and one QR Code per ticket (BR-30, BR-35). */
data class TicketSheet(
    val set: TicketSet,
    val items: List<TicketWithQr>,
)
