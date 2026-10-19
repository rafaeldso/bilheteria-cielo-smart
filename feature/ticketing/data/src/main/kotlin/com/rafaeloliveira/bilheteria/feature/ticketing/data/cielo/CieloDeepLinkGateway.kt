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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [PaymentGateway] over the Cielo `lio://` deep link (ADR-0010). Requests are fire-and-forget: the answer comes back
 * through [CieloCallbackActivity]. The intent rules (resolved package, allowlist, explicit intent, service before the
 * deep link) live in [DeepLinkOpener]: nothing is sent when the Cielo app is missing (AC-12, BR-14).
 */
internal class CieloDeepLinkGateway
    @Inject
    constructor(
        private val encoder: CieloRequestEncoder,
        private val credentials: CieloCredentials,
        private val opener: DeepLinkOpener,
        private val session: PaymentSession,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : PaymentGateway {
        override suspend fun requestPayment(purchase: Purchase): DataResult<Unit> =
            open(encoder.encodePayment(purchase, credentials.clientId, credentials.accessToken))

        override suspend fun requestLookup(page: Int): DataResult<Unit> =
            open(encoder.encodeLookup(page, credentials.clientId, credentials.accessToken))

        override fun finish() = session.stop()

        private suspend fun open(uri: String): DataResult<Unit> = withContext(ioDispatcher) { opener.open(uri) }
    }
