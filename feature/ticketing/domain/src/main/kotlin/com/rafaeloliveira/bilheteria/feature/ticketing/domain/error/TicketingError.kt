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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.error

import com.rafaeloliveira.bilheteria.core.common.AppError

/** Business failures of the ticketing feature (plan §5). Only safe data, never Cielo payloads. */
sealed interface TicketingError : AppError {
    data object CatalogUnavailable : TicketingError

    data object PurchaseAlreadyOpen : TicketingError

    data object TerminalAppUnavailable : TicketingError

    data object StorageFailure : TicketingError

    /** The purchase does not exist or is not approved: no tickets (BR-33, BR-36). */
    data object TicketsUnavailable : TicketingError

    /** The QR Code or an image file could not be produced (BR-36, BR-43). */
    data object TicketGenerationFailed : TicketingError
}
