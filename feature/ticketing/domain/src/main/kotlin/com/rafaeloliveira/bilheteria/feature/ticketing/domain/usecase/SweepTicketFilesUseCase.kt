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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketFiles
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketPrinter
import javax.inject.Inject

/**
 * Removes the ticket images an earlier process left behind (plan §7, information disclosure: the image holds the
 * purchase `reference` inside the QR Code). Skipped while a print job runs, because its image is in use.
 */
class SweepTicketFilesUseCase
    @Inject
    constructor(
        private val files: TicketFiles,
        private val printer: TicketPrinter,
    ) {
        suspend operator fun invoke() {
            if (printer.status.value !is PrintStatus.Printing) files.sweepStale()
        }
    }
