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
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.core.common.suspendRunCatching
import com.rafaeloliveira.bilheteria.feature.ticketing.data.di.CallbackScope
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase.HandlePaymentOutcomeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Logic of [CieloCallbackActivity] kept out of the Android class so it runs on the JVM: parses the raw `response`
 * parameter and stores the outcome (BR-11 to BR-14, BR-25). The work runs in an application-scoped coroutine, so it
 * completes even if the activity is destroyed. Neither the response nor an exception message is ever logged.
 */
internal class CieloCallbackHandler
    @Inject
    constructor(
        private val parser: CieloResponseParser,
        private val handlePaymentOutcome: HandlePaymentOutcomeUseCase,
        @param:CallbackScope private val scope: CoroutineScope,
        private val logger: Logger,
    ) {
        /** Handles [response] (the raw query parameter, null when absent) and calls [onDone] when it finishes. */
        fun handle(
            response: String?,
            onDone: () -> Unit,
        ) {
            scope
                .launch {
                    // Bounded so the translucent activity never stays over the app if storage hangs.
                    val result =
                        withTimeoutOrNull(TIMEOUT_MILLIS) {
                            suspendRunCatching { handlePaymentOutcome(parser.parse(response)) }
                        }
                    if (result?.getOrNull() !is DataResult.Success) {
                        // An open purchase stays in Room and the recovery (BR-15) resolves it. A late approval (BR-29)
                        // has none to recover: if it fails, the old Failed row stays as it was.
                        logger.error("payment_outcome_not_applied")
                    }
                }.invokeOnCompletion { onDone() }
        }

        private companion object {
            const val TIMEOUT_MILLIS = 10_000L
        }
    }
