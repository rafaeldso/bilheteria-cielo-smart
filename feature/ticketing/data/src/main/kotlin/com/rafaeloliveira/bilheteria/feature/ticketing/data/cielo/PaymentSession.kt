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

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rafaeloliveira.bilheteria.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Keeps the process alive while the Cielo app handles the payment (Cielo FAQ, research.md §12). */
internal interface PaymentSession {
    fun start()

    fun stop()
}

/** [PaymentSession] backed by [PaymentForegroundService]. */
internal class ForegroundServicePaymentSession
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val logger: Logger,
    ) : PaymentSession {
        override fun start() {
            try {
                ContextCompat.startForegroundService(context, serviceIntent())
            } catch (expected: IllegalStateException) {
                // Android 12+ refuses it when the app is in the background (ForegroundServiceStartNotAllowedException).
                // The service only lowers the odds of losing the callback; the payment still works, and recovery
                // (BR-15) covers a lost one, so this must not block the purchase.
                logger.warn("payment_service_not_started", expected)
            }
        }

        override fun stop() {
            context.stopService(serviceIntent())
        }

        private fun serviceIntent(): Intent = Intent(context, PaymentForegroundService::class.java)
    }
