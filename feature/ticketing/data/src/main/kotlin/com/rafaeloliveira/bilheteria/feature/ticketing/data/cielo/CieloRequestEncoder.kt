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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.io.encoding.Base64

/**
 * Builds the `lio://` URIs sent to the Cielo app (ADR-0010, research.md §12 and §13). Pure Kotlin: the gateway only
 * wraps the returned string in an `Intent(ACTION_VIEW)`.
 *
 * - Payment (BR-09): JSON with `accessToken`, `clientID`, `reference`, one item per purchase and `value` (total in
 *   cents, as a string) → Base64 → `lio://payment?request=…&urlCallback=order://response`. There is no `paymentCode`:
 *   the terminal asks the payment method (spec Q7). `unitPrice` is in cents (confirmed in the emulator, §13).
 * - Order listing (BR-15, BR-25): `lio://orders?request=…&urlCallback=order://response` with `pageSize` (at most 5) and
 *   the 0-based `page`.
 *
 * The Base64 uses the standard alphabet without line breaks (`kotlin.io.encoding`, available on every API level).
 * `+`, `/` and `=` are percent-encoded in the query so that `Uri.getQueryParameter` on the Cielo side returns the exact
 * Base64 (a raw `+` would turn into a space). The credentials are parameters (they come from `CieloCredentials`, task
 * T15) and never reach a log or an error.
 */
internal class CieloRequestEncoder
    @Inject
    constructor() {
        /** URI that opens the payment of [purchase] (AC-07). */
        fun encodePayment(
            purchase: Purchase,
            clientId: String,
            accessToken: String,
        ): String {
            val request =
                PaymentRequestDto(
                    accessToken = accessToken,
                    clientId = clientId,
                    reference = purchase.reference.value,
                    items =
                        listOf(
                            ItemRequestDto(
                                name = purchase.eventName,
                                quantity = purchase.quantity,
                                sku = purchase.eventId,
                                unitOfMeasure = UNIT_OF_MEASURE,
                                unitPrice = purchase.unitPrice.cents,
                            ),
                        ),
                    value = purchase.total.cents.toString(),
                )
            return uri(PAYMENT_HOST, json.encodeToString(request))
        }

        /** URI that lists one page of Cielo orders; [page] is the 0-based index (AC-14, AC-15). */
        fun encodeLookup(
            page: Int,
            clientId: String,
            accessToken: String,
        ): String {
            val request =
                LookupRequestDto(
                    pageSize = LOOKUP_PAGE_SIZE,
                    page = page,
                    clientId = clientId,
                    accessToken = accessToken,
                )
            return uri(ORDERS_HOST, json.encodeToString(request))
        }

        private fun uri(
            host: String,
            payload: String,
        ): String {
            val base64 = Base64.Default.encode(payload.encodeToByteArray())
            val query = base64.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D")
            return "lio://$host?request=$query&urlCallback=$CALLBACK"
        }

        internal companion object {
            const val LOOKUP_PAGE_SIZE = 5
            private const val PAYMENT_HOST = "payment"
            private const val ORDERS_HOST = "orders"
            private const val CALLBACK = "order://response"
            private const val UNIT_OF_MEASURE = "unidade"
            private val json = Json { encodeDefaults = true }
        }
    }

@Serializable
internal data class PaymentRequestDto(
    val accessToken: String,
    @SerialName("clientID") val clientId: String,
    val reference: String,
    val items: List<ItemRequestDto>,
    val value: String,
)

@Serializable
internal data class ItemRequestDto(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    val unitPrice: Long,
)

@Serializable
internal data class LookupRequestDto(
    val pageSize: Int,
    val page: Int,
    @SerialName("clientID") val clientId: String,
    val accessToken: String,
)
