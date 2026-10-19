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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.common.IoDispatcher
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.core.common.suspendRunCatching
import com.rafaeloliveira.bilheteria.feature.ticketing.data.di.CallbackScope
import com.rafaeloliveira.bilheteria.feature.ticketing.data.ticket.PrintImageStore
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintOutcome
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketPrinter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TicketPrinter] over `lio://print` (spec 002, BR-38 to BR-40, BR-44; plan §3, §4). One ticket image per call: Cielo
 * answers one `{code, message}` per call, so the next ticket is only sent once the previous one is confirmed, and a
 * failure leaves [PrintStatus.Failed] with how many were confirmed, which the retry resumes from (BR-40).
 *
 * It is a singleton, so the job survives a rotation and the screen only observes [status]. It is also the
 * [PrintResultSink] of the callback (R2): while [awaitingResult], an answer shaped like a print answer is *its* answer
 * and never reaches the payment flow. A step with no answer in [STEP_TIMEOUT_MILLIS] fails with `Timeout`; a late
 * answer is then dropped as unexpected. The image file is deleted when its step ends, whatever the result. Neither
 * paths,
 * URIs nor answers are logged.
 */
@Singleton
internal class CieloPrintCoordinator
    @Inject
    constructor(
        private val opener: DeepLinkOpener,
        private val encoder: CieloPrintEncoder,
        private val images: PrintImageStore,
        private val session: PaymentSession,
        @param:CallbackScope private val scope: CoroutineScope,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val logger: Logger,
    ) : TicketPrinter,
        PrintResultSink {
        private val state = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
        private val lock = Mutex()
        private var step: Step? = null

        @Volatile
        private var awaiting = false

        override val status: StateFlow<PrintStatus> = state.asStateFlow()

        override val awaitingResult: Boolean get() = awaiting

        override suspend fun print(
            set: TicketSet,
            from: Int,
        ): DataResult<Unit> =
            // Runs in the singleton's scope: if the screen leaves while the first image is being written, the job still
            // reaches a final state (waiting for the answer, or Failed) and no image is left behind.
            scope.async { startJob(set, from) }.await()

        private suspend fun startJob(
            set: TicketSet,
            from: Int,
        ): DataResult<Unit> =
            lock.withLock {
                val total = set.tickets.size
                if (state.value is PrintStatus.Printing || total == 0) return DataResult.Success(Unit)
                val first = from.coerceIn(0, total - 1)
                state.value = PrintStatus.Printing(set.purchaseId, printed = first, total = total)
                // Disk, MediaStore and the deep link are blocking calls: never on the caller's (main) thread (AP-25).
                withContext(ioDispatcher) {
                    images.sweepStale() // no job is running here, so anything left by an earlier process is garbage
                    sendStep(set, first)
                }
            }

        override fun acknowledge() {
            state.update { if (it is PrintStatus.Failed || it is PrintStatus.Done) PrintStatus.Idle else it }
        }

        override fun onPrintOutcome(outcome: PrintOutcome) {
            if (!awaiting) return
            scope.launch { lock.withLock { applyOutcome(outcome) } }
        }

        private suspend fun sendStep(
            set: TicketSet,
            index: Int,
        ): DataResult<Unit> {
            val path =
                when (val written = guarded { images.write(set, set.tickets[index]) }) {
                    is DataResult.Success -> written.value
                    is DataResult.Failure -> return failBeforeSending(set, index, written)
                }
            // The answer can arrive before open() returns: the sink must already be waiting (the step is set under
            // the lock).
            awaiting = true
            return when (val opened = guarded { opener.open(encoder.encodeImage(path)) }) {
                is DataResult.Failure -> {
                    awaiting = false
                    images.delete(path)
                    failBeforeSending(set, index, opened)
                }

                is DataResult.Success -> {
                    step = Step(set, index, path, scope.launch { timeoutOf(index) })
                    logger.info("print_started")
                    DataResult.Success(Unit)
                }
            }
        }

        /**
         * An unexpected failure of the store or the opener must end the job as `Unknown`, never leave it stuck in
         * `Printing` (the file is deleted by the caller's failure path). Cancellation is never swallowed.
         */
        private suspend fun <T> guarded(block: suspend () -> DataResult<T>): DataResult<T> =
            suspendRunCatching { block() }.getOrElse {
                logger.warn("print_step_unexpected_failure")
                DataResult.Failure(TicketingError.TicketGenerationFailed)
            }

        private suspend fun timeoutOf(index: Int) {
            delay(STEP_TIMEOUT_MILLIS)
            lock.withLock { if (step?.index == index) applyOutcome(null) }
        }

        /** [outcome] null means the step timed out. Runs under [lock]. */
        private suspend fun applyOutcome(outcome: PrintOutcome?) {
            val current = step ?: return
            current.timeout.cancel()
            step = null
            awaiting = false
            images.delete(current.path)
            val total = current.set.tickets.size
            val printed = current.index + 1
            when (outcome) {
                PrintOutcome.Success -> {
                    if (printed < total) {
                        state.value = PrintStatus.Printing(current.set.purchaseId, printed, total)
                        sendStep(current.set, printed)
                    } else {
                        session.stop()
                        logger.info("print_done")
                        state.value = PrintStatus.Done(current.set.purchaseId, total)
                    }
                }

                else -> {
                    fail(current.set, current.index, reasonOf(outcome))
                }
            }
        }

        private fun reasonOf(outcome: PrintOutcome?): PrintFailureReason =
            when (outcome) {
                null -> PrintFailureReason.Timeout
                PrintOutcome.NoPaper -> PrintFailureReason.NoPaper
                PrintOutcome.PrinterError -> PrintFailureReason.PrinterError
                else -> PrintFailureReason.Unknown
            }

        private fun failBeforeSending(
            set: TicketSet,
            index: Int,
            failure: DataResult.Failure,
        ): DataResult<Unit> {
            val reason =
                if (failure.error == TicketingError.TerminalAppUnavailable) {
                    PrintFailureReason.TerminalAppUnavailable
                } else {
                    PrintFailureReason.Unknown
                }
            fail(set, index, reason)
            return failure
        }

        private fun fail(
            set: TicketSet,
            printed: Int,
            reason: PrintFailureReason,
        ) {
            session.stop()
            logger.warn("print_failed")
            state.value = PrintStatus.Failed(set.purchaseId, reason, printed = printed, total = set.tickets.size)
        }

        private class Step(
            val set: TicketSet,
            val index: Int,
            val path: String,
            val timeout: Job,
        )

        internal companion object {
            /** How long one ticket may take before the job fails with `Timeout`. */
            const val STEP_TIMEOUT_MILLIS = 15_000L
        }
    }
