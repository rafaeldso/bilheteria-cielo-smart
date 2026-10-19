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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import javax.inject.Inject

/**
 * Opens a `lio://` deep link in the Cielo app (ADR-0010), shared by the payment gateway and the ticket printer.
 *
 * Security (plan §7, STRIDE information disclosure): the intent is only sent to the package that Android resolves for
 * it AND that is in [ALLOWED_PACKAGES], and it is made explicit with `setPackage`. Anything else (no app, another app
 * claiming `lio://`, an activity that cannot start) is `TicketingError.TerminalAppUnavailable`: nothing was sent. The
 * foreground service starts before the deep link and only once the Cielo app was found. The URI is never logged.
 */
internal class DeepLinkOpener
    @Inject
    constructor(
        private val launcher: TerminalAppLauncher,
        private val session: PaymentSession,
        private val logger: Logger,
    ) {
        fun open(uri: String): DataResult<Unit> {
            val resolved = launcher.resolvePackage(uri)
            return when {
                resolved == null -> {
                    unavailable("terminal_app_not_found")
                }

                resolved !in ALLOWED_PACKAGES -> {
                    unavailable("terminal_app_not_allowed")
                }

                else -> {
                    session.start()
                    if (launcher.launch(uri, resolved)) {
                        DataResult.Success(Unit)
                    } else {
                        session.stop()
                        unavailable("terminal_app_not_started")
                    }
                }
            }
        }

        private fun unavailable(reason: String): DataResult<Unit> {
            logger.warn(reason)
            return DataResult.Failure(TicketingError.TerminalAppUnavailable)
        }

        internal companion object {
            /** Cielo terminal app and the emulator's order service (research.md §13). */
            val ALLOWED_PACKAGES: Set<String> = setOf("com.ads.lio.uriappclient", "br.com.cielosmart.orderservice")
        }
    }
