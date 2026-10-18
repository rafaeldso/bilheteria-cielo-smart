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

import android.app.Application
import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase.RecordingLogger
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** The Android seams of the gateway (resolution, explicit intent, foreground service) on Robolectric. */
@RunWith(RobolectricTestRunner::class)
class AndroidTerminalIntegrationTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val uri = "lio://payment?request=ZmFrZQ%3D%3D&urlCallback=order://response"

    private fun installCieloApp(pkg: String) {
        val component = ComponentName(pkg, "$pkg.UriActivity")
        val packageManager = shadowOf(application.packageManager)
        packageManager.addActivityIfNotPresent(component)
        packageManager.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("lio")
            },
        )
    }

    @Test
    fun `AC-12 - dado nenhum app tratando lio quando resolver entao nao ha pacote`() {
        assertNull(AndroidTerminalAppLauncher(application).resolvePackage(uri))
    }

    @Test
    fun `AC-07 - dado app Cielo instalado quando resolver entao devolve o pacote que trata lio`() {
        installCieloApp("com.ads.lio.uriappclient")

        assertEquals("com.ads.lio.uriappclient", AndroidTerminalAppLauncher(application).resolvePackage(uri))
    }

    @Test
    fun `AC-07 - dado app Cielo instalado quando abrir entao intent explicita para o pacote em nova task`() {
        installCieloApp("com.ads.lio.uriappclient")

        val launched = AndroidTerminalAppLauncher(application).launch(uri, "com.ads.lio.uriappclient")

        val started = shadowOf(application).nextStartedActivity
        assertTrue(launched)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(uri, started.dataString)
        assertEquals("com.ads.lio.uriappclient", started.`package`)
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `AC-12 - dado app sem activity para o deep link quando abrir entao devolve false sem excecao`() {
        shadowOf(application).checkActivities(true)

        assertFalse(AndroidTerminalAppLauncher(application).launch(uri, "com.ads.lio.uriappclient"))
    }

    @Test
    fun `AC-07 - dado pagamento iniciado quando abrir e finalizar entao inicia e para o servico em primeiro plano`() {
        val session = ForegroundServicePaymentSession(application, RecordingLogger())

        session.start()
        val started = shadowOf(application).nextStartedService
        session.stop()
        val stopped = shadowOf(application).nextStoppedService

        assertEquals(PaymentForegroundService::class.java.name, started.component?.className)
        assertEquals(PaymentForegroundService::class.java.name, stopped.component?.className)
    }

    @Test
    fun `AC-07 - dado servico criado quando iniciar entao fica em primeiro plano com notificacao sem valor`() {
        val service = Robolectric.buildService(PaymentForegroundService::class.java).create().get()

        val notification = shadowOf(service).lastForegroundNotification
        assertNotNull(notification)
        assertTrue(shadowOf(service).isLastForegroundNotificationAttached)
        val extras = notification.extras
        assertEquals("Pagamento em andamento", extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals("Aguardando a conclusão na maquininha", extras.getCharSequence(Notification.EXTRA_TEXT))
        assertEquals(Notification.VISIBILITY_SECRET, notification.visibility)
    }
}
