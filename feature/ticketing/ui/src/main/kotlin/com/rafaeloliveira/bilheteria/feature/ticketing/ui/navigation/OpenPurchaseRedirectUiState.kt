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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation

import androidx.compose.runtime.Immutable

/**
 * Whether the app opened with a purchase still open (`Pending` or `Recovering`) that the user must be taken back to
 * (BR-15, ui-spec flow "compra em andamento sem resultado"). It is read once, when the activity starts.
 */
@Immutable
internal sealed interface OpenPurchaseRedirectUiState {
    /** The open purchase is still being read. */
    data object Checking : OpenPurchaseRedirectUiState

    /** Nothing to resume, the open purchase could not be read (the purchase screen shows that), or it was handled. */
    data object None : OpenPurchaseRedirectUiState

    /** A purchase of [eventId] is open: SCR-02 resumes it (`Recovering`, BR-15). */
    data class ToPurchase(
        val eventId: String,
    ) : OpenPurchaseRedirectUiState
}
