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
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CieloDeepLinkGatewayTest {
    private val events = mutableListOf<String>()
    private val launcher = FakeLauncher(events)
    private val session = FakeSession(events)
    private val logger = CapturingLogger()
    private val encoder = CieloRequestEncoder()
    private val credentials = CieloCredentials(TEST_CLIENT_ID, TEST_ACCESS_TOKEN)

    private fun gateway(dispatcher: CoroutineDispatcher) =
        CieloDeepLinkGateway(encoder, credentials, launcher, session, dispatcher, logger)

    @ParameterizedTest
    @ValueSource(strings = ["com.ads.lio.uriappclient", "br.com.cielosmart.orderservice"])
    fun `AC-07 - dado app Cielo na allowlist quando pagar entao inicia o servico antes do deep link explicito`(
        pkg: String,
    ) = runTest {
        launcher.resolved = pkg
        val purchase = aPurchase()

        val result = gateway(StandardTestDispatcher(testScheduler)).requestPayment(purchase)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(listOf("resolve", "service-start", "launch:$pkg"), events)
        assertEquals(encoder.encodePayment(purchase, TEST_CLIENT_ID, TEST_ACCESS_TOKEN), launcher.launchedUri)
        assertEquals(pkg, launcher.launchedPackage)
    }

    @Test
    fun `AC-12 - dado app Cielo ausente quando pagar entao TerminalAppUnavailable sem abrir nada`() =
        runTest {
            launcher.resolved = null

            val result = gateway(StandardTestDispatcher(testScheduler)).requestPayment(aPurchase())

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(listOf("resolve"), events)
        }

    @Test
    fun `AC-12 - dado pacote fora da allowlist quando pagar entao TerminalAppUnavailable sem startActivity`() =
        runTest {
            launcher.resolved = "com.malicious.lioimpostor"

            val result = gateway(StandardTestDispatcher(testScheduler)).requestPayment(aPurchase())

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertFalse(events.any { it.startsWith("launch") }, "the token must not be sent to an unknown app")
            assertFalse(events.contains("service-start"))
        }

    @Test
    fun `AC-12 - dado activity que nao inicia quando pagar entao TerminalAppUnavailable e servico parado`() =
        runTest {
            launcher.resolved = "com.ads.lio.uriappclient"
            launcher.launchSucceeds = false

            val result = gateway(StandardTestDispatcher(testScheduler)).requestPayment(aPurchase())

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
            assertEquals(listOf("resolve", "service-start", "launch:com.ads.lio.uriappclient", "service-stop"), events)
        }

    @Test
    fun `AC-15 - dado compra aberta ao reabrir quando consultar entao abre lio orders na allowlist`() =
        runTest {
            launcher.resolved = "br.com.cielosmart.orderservice"

            val result = gateway(StandardTestDispatcher(testScheduler)).requestLookup(page = 2)

            assertEquals(DataResult.Success(Unit), result)
            assertEquals(encoder.encodeLookup(2, TEST_CLIENT_ID, TEST_ACCESS_TOKEN), launcher.launchedUri)
            assertEquals(listOf("resolve", "service-start", "launch:br.com.cielosmart.orderservice"), events)
        }

    @Test
    fun `AC-15 - dado app Cielo ausente quando consultar entao TerminalAppUnavailable`() =
        runTest {
            launcher.resolved = null

            val result = gateway(StandardTestDispatcher(testScheduler)).requestLookup(page = 0)

            assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
        }

    @Test
    fun `AC-07 - dado pagamento concluido quando finalizar entao para o servico em primeiro plano`() =
        runTest {
            gateway(StandardTestDispatcher(testScheduler)).finish()

            assertEquals(listOf("service-stop"), events)
        }

    @Test
    fun `AC-12 - dado falha ao abrir a Cielo quando pagar entao o log nao tem uri nem token`() =
        runTest {
            launcher.resolved = "com.malicious.lioimpostor"

            gateway(StandardTestDispatcher(testScheduler)).requestPayment(aPurchase())

            assertTrue(logger.messages.isNotEmpty())
            assertTrue(logger.messages.none { TEST_ACCESS_TOKEN in it || "lio://" in it || TEST_CLIENT_ID in it })
        }

    private class FakeLauncher(
        private val events: MutableList<String>,
    ) : TerminalAppLauncher {
        var resolved: String? = null
        var launchSucceeds = true
        var launchedUri: String? = null
        var launchedPackage: String? = null

        override fun resolvePackage(uri: String): String? {
            events += "resolve"
            return resolved
        }

        override fun launch(
            uri: String,
            packageName: String,
        ): Boolean {
            events += "launch:$packageName"
            launchedUri = uri
            launchedPackage = packageName
            return launchSucceeds
        }
    }

    private class FakeSession(
        private val events: MutableList<String>,
    ) : PaymentSession {
        override fun start() {
            events += "service-start"
        }

        override fun stop() {
            events += "service-stop"
        }
    }

    private class CapturingLogger : Logger {
        val messages = mutableListOf<String>()

        override fun info(message: String) {
            messages += message
        }

        override fun warn(
            message: String,
            throwable: Throwable?,
        ) {
            messages += message
        }

        override fun error(
            message: String,
            throwable: Throwable?,
        ) {
            messages += message
        }
    }
}
