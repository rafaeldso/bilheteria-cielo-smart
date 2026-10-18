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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Receives `order://response?response=<base64>` from the Cielo app (plan §3, §4). It is exported, so everything in the
 * intent is untrusted input (plan §7): it only reads the `response` parameter and hands it to
 * [CieloCallbackHandler] (parser, then `HandlePaymentOutcomeUseCase`, which checks it against the open purchase:
 * reference and amount). It never navigates by parameter: it just brings the app back (`MainActivity` reacts to
 * Room) and finishes. It has no UI (translucent theme). The raw response is never logged.
 *
 * Hilt `@AndroidEntryPoint` needs `ComponentActivity`; this plain `Activity` reads its dependencies from an
 * [EntryPoint] instead, which keeps the module free of AndroidX activity dependencies.
 */
internal class CieloCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(callback: Intent?) {
        EntryPointAccessors
            .fromApplication(applicationContext, CieloCallbackEntryPoint::class.java)
            .callbackHandler()
            .handle(responseParameter(callback)) { runOnUiThread(::returnToApp) }
    }

    private fun responseParameter(callback: Intent?): String? {
        val data = callback?.data ?: return null
        return if (data.scheme == CALLBACK_SCHEME &&
            data.host == CALLBACK_HOST
        ) {
            data.getQueryParameter(PARAMETER)
        } else {
            null
        }
    }

    private fun returnToApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
        finish()
    }

    private companion object {
        const val CALLBACK_SCHEME = "order"
        const val CALLBACK_HOST = "response"
        const val PARAMETER = "response"
    }
}

/** Dependencies of [CieloCallbackActivity], which is not a Hilt-injected component. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface CieloCallbackEntryPoint {
    fun callbackHandler(): CieloCallbackHandler
}
