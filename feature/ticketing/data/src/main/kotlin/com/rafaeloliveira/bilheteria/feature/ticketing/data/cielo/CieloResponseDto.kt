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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Whitelist of what the app reads from a Cielo response (plan §4, Data). Everything else in the payload, in particular
 * `payments[].mask`, `paymentFields.{pan, bin, document, clientName, finalCryptogram, signatureBytes}`, `merchantCode`
 * and `accessKey` (research.md §12, §13), is not declared here, so it is skipped by `ignoreUnknownKeys` and can never
 * reach the domain model, a log or the database.
 *
 * Scalars that the documentation shows both as number and as string are read as [JsonPrimitive] and converted by
 * [text]; a response with the wrong shape fails to decode and becomes `PaymentOutcome.Unreadable`.
 */
@Serializable
internal data class CieloResponseDto(
    /** Present only in the error shape `{code, reason}`. */
    val code: JsonPrimitive? = null,
    val id: JsonPrimitive? = null,
    val reference: String? = null,
    val payments: List<CieloPaymentDto>? = null,
    /** Present only in the `lio://orders` listing. */
    val results: List<CieloOrderDto>? = null,
    val currentPage: JsonPrimitive? = null,
    val totalPages: JsonPrimitive? = null,
)

@Serializable
internal data class CieloOrderDto(
    val id: JsonPrimitive? = null,
    val reference: String? = null,
    val payments: List<CieloPaymentDto>? = null,
)

@Serializable
internal data class CieloPaymentDto(
    val amount: JsonPrimitive? = null,
    val authCode: JsonPrimitive? = null,
    val cieloCode: JsonPrimitive? = null,
    val brand: JsonPrimitive? = null,
    val paymentFields: CieloPaymentFieldsDto? = null,
)

@Serializable
internal data class CieloPaymentFieldsDto(
    val statusCode: JsonPrimitive? = null,
)

/** The primitive as text, or null when absent or JSON `null`. */
internal fun JsonPrimitive?.text(): String? = if (this == null || this is JsonNull) null else content
