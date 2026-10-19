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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.ticket

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet

/**
 * Where the ticket image goes so the Cielo app can read it by path (`PRINT_IMAGE` takes a path, research.md §1). The
 * file is temporary: the printer deletes it as soon as the step ends, and [sweepStale] removes what a dead process
 * left.
 */
internal interface PrintImageStore {
    /**
     * Writes the image of [ticket] (384 px wide) and returns its absolute path. Fails with
     * `TicketingError.TerminalAppUnavailable` when the device cannot print (API below 29) and with
     * `TicketingError.TicketGenerationFailed` when the image cannot be produced or written.
     */
    suspend fun write(
        set: TicketSet,
        ticket: Ticket,
    ): DataResult<String>

    /** Deletes the file at [path]; never throws. */
    fun delete(path: String)

    /** Deletes the print images left behind (called when the app starts). */
    fun sweepStale()
}
