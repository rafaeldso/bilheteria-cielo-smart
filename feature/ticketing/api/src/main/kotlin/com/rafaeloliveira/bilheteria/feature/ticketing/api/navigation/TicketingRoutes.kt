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
package com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation

import kotlinx.serialization.Serializable

/** SCR-01 Eventos à venda: the start of the ticketing flow. */
@Serializable
public data object EventListRoute

/**
 * SCR-02 Compra for the event with id [eventId]. The property name is the key of the `SavedStateHandle` entry the
 * purchase screen reads, so renaming it is a breaking change (covered by a test in `:feature:ticketing:ui`).
 */
@Serializable
public data class PurchaseRoute(
    public val eventId: String,
)

/**
 * SCR-03 Comprovante of the purchase with id [purchaseId] (the Cielo `reference`). Only an id travels in the route;
 * the screen reads the record (BR-19). The property name is the `SavedStateHandle` key, as in [PurchaseRoute].
 */
@Serializable
public data class ReceiptRoute(
    public val purchaseId: String,
)

/** SCR-04 Histórico de compras. */
@Serializable
public data object PurchaseHistoryRoute
