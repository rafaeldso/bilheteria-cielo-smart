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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintOutcome

/**
 * Where a `lio://print` answer goes (spec 002, plan §4). The callback activity also receives payment answers, and the
 * two share the `code` field, so the handler routes by this state: an answer shaped like a print answer is a print
 * answer only while a print job is [awaitingResult]; otherwise it must never reach the payment flow.
 */
internal interface PrintResultSink {
    /** True while a print job waits for the answer of the step it sent. */
    val awaitingResult: Boolean

    /** Delivers the answer of the awaited step. Ignored when nothing is awaited. */
    fun onPrintOutcome(outcome: PrintOutcome)
}
