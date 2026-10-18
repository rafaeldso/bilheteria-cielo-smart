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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.history

/** Events of SCR-04 (spec section 6). */
internal sealed interface PurchaseHistoryUiEvent {
    /** "Tentar de novo" after an error (BR-21). */
    data object OnRetry : PurchaseHistoryUiEvent

    /** The operator picked a purchase; the Route opens its receipt by id (BR-19). */
    data class OnPurchaseSelected(
        val id: String,
    ) : PurchaseHistoryUiEvent
}
