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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.ShareBundle
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketFiles
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketPrinter
import javax.inject.Inject

/**
 * Prepares the ticket images of an approved purchase for the system share sheet (BR-41, BR-42). Sharing is unavailable
 * while a print job runs (D3, BR-39): a share failure must not release a second print, so the request is ignored and
 * the result is null. The app never learns who receives the images: the destination is chosen in the system menu.
 */
class ShareTicketsUseCase
    @Inject
    constructor(
        private val loadTicketSet: LoadTicketSetUseCase,
        private val files: TicketFiles,
        private val printer: TicketPrinter,
    ) {
        /** The images to share, or null when ignored because a print job is running. */
        suspend operator fun invoke(purchaseId: String): DataResult<ShareBundle?> {
            if (printer.status.value is PrintStatus.Printing) return DataResult.Success(null)
            return when (val loaded = loadTicketSet(purchaseId)) {
                is DataResult.Failure -> loaded
                is DataResult.Success -> files.prepareShare(loaded.value)
            }
        }
    }
