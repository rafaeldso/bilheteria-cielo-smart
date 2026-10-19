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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

/** Events of the receipt screen (SCR-03). */
internal sealed interface ReceiptUiEvent {
    /** "Nova venda": back to the event list. The navigation itself belongs to the Route. */
    data object OnNewSale : ReceiptUiEvent

    /** "Ver ingressos" (only when approved): opens SCR-05 (spec 002, BR-34). The navigation belongs to the Route. */
    data object OnOpenTickets : ReceiptUiEvent

    /** "Tentar novamente" after an error: reads the record again (BR-28, AC-27). */
    data object OnRetry : ReceiptUiEvent
}
