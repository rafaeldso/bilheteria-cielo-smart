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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.LookupOrder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PaymentOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseReference
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TransactionData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import kotlin.io.encoding.Base64

/**
 * Turns the `response` query parameter of `order://response` into a [PaymentOutcome] (BR-11 to BR-14, BR-25). The
 * response is untrusted input (STRIDE: tampering, denial of service): this class never throws, never logs and never
 * returns data it did not validate.
 *
 * Pipeline: size limit ([MAX_RESPONSE_LENGTH], checked before decoding) → Base64 (tolerates the line breaks that
 * `Base64.DEFAULT` adds, research.md §13) → JSON with `ignoreUnknownKeys` into the whitelist DTO
 * ([CieloResponseDto]), which is the only thing that is read. Classification uses the JSON, never `responsecode` nor
 * the order `status` (the listing says `ENTERED` even for paid orders, §13):
 * - `results[]` present → [PaymentOutcome.LookupPage] of the orders that carry a `reference`.
 * - `code` present → error (`1` [PaymentOutcome.Cancelled], `3` [PaymentOutcome.Declined], `4`
 *   [PaymentOutcome.Failed] with [FailureReason.Authentication], `2` and unknown codes [PaymentOutcome.Failed] with
 *   [FailureReason.Generic]); the mapping and its reasons are in the KDoc of [PaymentOutcome].
 * - Otherwise a payment order: [PaymentOutcome.Approved] only with the echoed `reference`, the order `id` and a
 *   payment with `paymentFields.statusCode` 1 (or 0, Pix) that has `amount`, `authCode` and `cieloCode`. `brand` may be
 *   empty and becomes null.
 * - Anything else (not Base64, not JSON, too big, missing fields, payment without an approved status) →
 *   [PaymentOutcome.Unreadable]: the charge may have happened, so the purchase goes to recovery (BR-25).
 *
 * Listing pages: `lio://orders` answers `currentPage` starting at 1 for the `page: 0` request (§13), so the domain
 * `page` (0-based index that was requested) is `currentPage - 1`. A listed order that has a paid payment but lacks
 * `id`, `authCode` or `cieloCode` makes the whole page [PaymentOutcome.Unreadable]: reporting it as "not paid" could
 * allow a second charge.
 */
internal class CieloResponseParser
    @Inject
    constructor() {
        /** Interprets [encoded] (the raw `response` parameter); null, oversized or corrupt input is Unreadable. */
        fun parse(encoded: String?): PaymentOutcome {
            if (encoded == null || encoded.length > MAX_RESPONSE_LENGTH) return PaymentOutcome.Unreadable
            return try {
                classify(json.decodeFromString<CieloResponseDto>(decode(encoded))) ?: PaymentOutcome.Unreadable
            } catch (expected: IllegalArgumentException) {
                // Base64, JSON shape and domain invariants all fail with IllegalArgumentException; the cause may hold
                // payload fragments, so it is dropped on purpose.
                PaymentOutcome.Unreadable
            }
        }

        /**
         * Reads [encoded] as a `lio://print` answer, or null when it is not shaped like one (spec 002, plan §4).
         * Print-shaped: a numeric `code` with a `message` or a `reason`, and none of the payment fields (`id`,
         * `reference`, `payments`, `results`). A [PrintResponse.certain] answer has `message` and no `reason`:
         * only a print answer looks like that. One with `reason` only is **ambiguous**: it is also the shape of a
         * payment error (`{code:1, reason}`) and of the print file error (`{code:2, reason}`), so the caller
         * decides by state. Never throws and never returns the payload.
         */
        fun parsePrint(encoded: String?): PrintResponse? {
            if (encoded == null || encoded.length > MAX_RESPONSE_LENGTH) return null
            return try {
                printShaped(json.decodeFromString<CieloPrintResponseDto>(decode(encoded)))
            } catch (expected: IllegalArgumentException) {
                null // Base64 and JSON failures: the payload is dropped on purpose
            }
        }

        private fun printShaped(dto: CieloPrintResponseDto): PrintResponse? {
            val code = dto.code.text()?.toIntOrNull()
            val hasText = dto.message != null || dto.reason != null
            if (code == null || !hasText || dto.isPaymentShaped()) return null
            return PrintResponse(
                outcome = printOutcomeOf(code, hasMessage = dto.message != null),
                certain = dto.isPrintOnly(),
            )
        }

        private fun printOutcomeOf(
            code: Int,
            hasMessage: Boolean,
        ): PrintOutcome =
            when (code) {
                PRINT_SUCCESS -> PrintOutcome.Success
                PRINT_WITHOUT_PAPER -> PrintOutcome.NoPaper
                PRINT_ERROR -> if (hasMessage) PrintOutcome.PrinterError else PrintOutcome.FileUnreadable
                else -> PrintOutcome.Unreadable
            }

        private fun CieloPrintResponseDto.isPaymentShaped(): Boolean =
            id != null || reference != null || payments != null || results != null

        private fun CieloPrintResponseDto.isPrintOnly(): Boolean = message != null && reason == null

        private fun decode(encoded: String): String {
            val clean = encoded.filterNot { it == '\n' || it == '\r' }.replace(' ', '+')
            return Base64.Default.decode(clean).decodeToString()
        }

        private fun classify(dto: CieloResponseDto): PaymentOutcome? =
            when {
                dto.results != null -> toLookupPage(dto, dto.results)
                dto.code != null -> toError(dto.code)
                else -> toApproved(dto)
            }

        private fun toError(code: JsonPrimitive): PaymentOutcome? =
            when (code.text()?.toIntOrNull()) {
                null -> null
                CODE_CANCELLED -> PaymentOutcome.Cancelled(reference = null)
                CODE_PAYMENT_ERROR -> PaymentOutcome.Declined(reference = null)
                CODE_AUTHENTICATION -> PaymentOutcome.Failed(reference = null, reason = FailureReason.Authentication)
                else -> PaymentOutcome.Failed(reference = null, reason = FailureReason.Generic)
            }

        private fun toApproved(dto: CieloResponseDto): PaymentOutcome? {
            val reference = dto.reference?.takeIf { it.isNotBlank() }?.let(::PurchaseReference) ?: return null
            return (approval(dto.id.text(), reference, dto.payments) as? Approval.Paid)?.approved
        }

        private fun toLookupPage(
            dto: CieloResponseDto,
            results: List<CieloOrderDto>,
        ): PaymentOutcome? {
            val currentPage = dto.currentPage.text()?.toIntOrNull()
            val totalPages = dto.totalPages.text()?.toIntOrNull()
            val orders = lookupOrders(results)
            if (currentPage == null || totalPages == null || orders == null) return null
            return PaymentOutcome.LookupPage(
                page = (currentPage - FIRST_CIELO_PAGE).coerceAtLeast(0),
                totalPages = totalPages.coerceAtLeast(0),
                orders = orders,
            )
        }

        /** Orders that carry a `reference`; null when one of them is paid but incomplete (see the class KDoc). */
        private fun lookupOrders(results: List<CieloOrderDto>): List<LookupOrder>? {
            val orders = results.filter { !it.reference.isNullOrBlank() }.map(::toLookupOrder)
            return if (orders.any { it == null }) null else orders.filterNotNull()
        }

        private fun toLookupOrder(order: CieloOrderDto): LookupOrder? {
            val reference = PurchaseReference(order.reference.orEmpty())
            return when (val approval = approval(order.id.text(), reference, order.payments)) {
                Approval.NotPaid -> LookupOrder(reference, approved = null)
                Approval.Incomplete -> null
                is Approval.Paid -> LookupOrder(reference, approval.approved)
            }
        }

        private fun approval(
            orderId: String?,
            reference: PurchaseReference,
            payments: List<CieloPaymentDto>?,
        ): Approval {
            val paid = payments.orEmpty().firstOrNull { it.paymentFields?.statusCode.text() in APPROVED_STATUS_CODES }
            if (paid == null) return Approval.NotPaid
            val approved = approvedOf(orderId, reference, paid)
            return if (approved == null) Approval.Incomplete else Approval.Paid(approved)
        }

        private fun approvedOf(
            orderId: String?,
            reference: PurchaseReference,
            paid: CieloPaymentDto,
        ): PaymentOutcome.Approved? {
            val amount =
                paid.amount
                    .text()
                    ?.toLongOrNull()
                    ?.takeIf { it >= 0 } ?: return null
            val (id, authCode, nsu) = listOf(orderId, paid.authCode.text(), paid.cieloCode.text()).map { it.nonBlank() }
            if (id == null || authCode == null || nsu == null) return null
            val brand = paid.brand.text().nonBlank()
            return PaymentOutcome.Approved(reference, Money(amount), TransactionData(id, authCode, nsu, brand))
        }

        private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

        private sealed interface Approval {
            data object NotPaid : Approval

            data object Incomplete : Approval

            data class Paid(
                val approved: PaymentOutcome.Approved,
            ) : Approval
        }

        internal companion object {
            /** Maximum length of the Base64 `response` accepted; a page of 5 orders is far below it (DoS limit). */
            const val MAX_RESPONSE_LENGTH = 256 * 1024
            private const val CODE_CANCELLED = 1
            private const val CODE_PAYMENT_ERROR = 3
            private const val CODE_AUTHENTICATION = 4
            private const val FIRST_CIELO_PAGE = 1
            private val APPROVED_STATUS_CODES = setOf("0", "1")
            private const val PRINT_SUCCESS = 0
            private const val PRINT_WITHOUT_PAPER = 1
            private const val PRINT_ERROR = 2
            private val json = Json { ignoreUnknownKeys = true }
        }
    }

/** A `lio://print` answer: [certain] is true when only a print answer has this shape (see `parsePrint`). */
internal data class PrintResponse(
    val outcome: PrintOutcome,
    val certain: Boolean,
)
