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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.tickets

/** Events of SCR-05 (spec 002, section 6). */
internal sealed interface TicketsUiEvent {
    /** "Tentar novamente" in [TicketsUiState.Error]: reads the purchase and encodes the QR Codes again (BR-36). */
    data object OnRetry : TicketsUiEvent

    /** "Imprimir ingressos" (BR-38); ignored while a print job runs (BR-39). */
    data object OnPrint : TicketsUiEvent

    /** "Compartilhar ingressos" (BR-41); ignored while a print job runs (D3). */
    data object OnShare : TicketsUiEvent

    /** "Tentar novamente" after a failed print or share: repeats that action (BR-40, BR-43). */
    data object OnRetryAction : TicketsUiEvent

    /** Technical: the Route opened the share sheet, so the one-shot signal is consumed. No acceptance criterion. */
    data object OnShareLaunched : TicketsUiEvent

    /** Technical: the system could not open the share sheet (no app, refused): shows the share failure (BR-43). */
    data object OnShareLaunchFailed : TicketsUiEvent

    /** Technical: the "Ingressos impressos" notice ended. No acceptance criterion. */
    data object OnActionDismissed : TicketsUiEvent
}
