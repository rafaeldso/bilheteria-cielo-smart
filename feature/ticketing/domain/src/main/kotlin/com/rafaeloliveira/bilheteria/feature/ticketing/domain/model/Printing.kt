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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.model

/** Why a ticket print job stopped (BR-40). */
enum class PrintFailureReason {
    /** Cielo app missing, or a device that cannot print (API below 29). */
    TerminalAppUnavailable,

    /** Cielo answered `code 1` ("WITHOUT PAPER"). */
    NoPaper,

    /** Cielo answered `code 2`. */
    PrinterError,

    /** Unreadable answer, unreadable image file, or any other failure. */
    Unknown,

    /** No answer in time. */
    Timeout,
}

/** State of the ticket print job, held by the data layer so it survives screen rotation (BR-38 to BR-40, BR-44). */
sealed interface PrintStatus {
    data object Idle : PrintStatus

    /** [printed] of [total] tickets of [purchaseId] confirmed by Cielo so far; a new request is ignored (BR-39). */
    data class Printing(
        val purchaseId: String,
        val printed: Int,
        val total: Int,
    ) : PrintStatus

    /** The job stopped after [printed] confirmed tickets; a retry resumes from there (BR-40). */
    data class Failed(
        val purchaseId: String,
        val reason: PrintFailureReason,
        val printed: Int,
        val total: Int,
    ) : PrintStatus

    /** Every ticket was confirmed (BR-44). */
    data class Done(
        val purchaseId: String,
        val total: Int,
    ) : PrintStatus
}

/**
 * One answer of the `lio://print` deep link, already interpreted by the data layer. Cielo answers `{code, message}`:
 * `0` success, `1` without paper, `2` error (research.md §1); a file it cannot read answers `{code:2, reason}`.
 */
sealed interface PrintOutcome {
    data object Success : PrintOutcome

    data object NoPaper : PrintOutcome

    data object PrinterError : PrintOutcome

    /** The image file could not be read by the Cielo app (EACCES, ENOENT). */
    data object FileUnreadable : PrintOutcome

    data object Unreadable : PrintOutcome
}
