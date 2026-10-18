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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Small seam over `PackageManager`/`startActivity` so the gateway logic runs on the plain JVM. */
internal interface TerminalAppLauncher {
    /** Package of the activity that would handle [uri], or null when no app does (Cielo app not installed). */
    fun resolvePackage(uri: String): String?

    /** Starts [uri] as an explicit intent to [packageName]; false when the activity cannot be started. */
    fun launch(
        uri: String,
        packageName: String,
    ): Boolean
}

/**
 * Real implementation. The URI carries the access token, so it is only parsed into an `Intent`: it is never logged
 * and never put in an exception. Resolution depends on the `<queries>` of the manifest (package visibility).
 */
internal class AndroidTerminalAppLauncher
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : TerminalAppLauncher {
        override fun resolvePackage(uri: String): String? =
            viewIntent(uri).resolveActivity(context.packageManager)?.packageName

        override fun launch(
            uri: String,
            packageName: String,
        ): Boolean =
            try {
                // setPackage makes the intent explicit: no other app can intercept the payment request and its token.
                context.startActivity(viewIntent(uri).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (expected: ActivityNotFoundException) {
                false // the message of the exception carries the intent (and the token): dropped on purpose
            } catch (expected: SecurityException) {
                false
            } catch (expected: IllegalArgumentException) {
                false // e.g. a binder failure wrapped by the framework: its message may carry the intent too
            }

        private fun viewIntent(uri: String): Intent = Intent(Intent.ACTION_VIEW, uri.toUri())
    }
