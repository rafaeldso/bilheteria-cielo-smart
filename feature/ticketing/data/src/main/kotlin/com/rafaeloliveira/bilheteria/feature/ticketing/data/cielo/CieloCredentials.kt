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

/**
 * Cielo Client-ID and Access Token (plan §4, §7). They come from the Gradle properties `cielo.clientId` and
 * `cielo.accessToken` through `BuildConfig` (fictitious defaults, Q12) and are embedded in the APK: an accepted risk.
 * [toString] is redacted so the credentials never reach a log, a crash report or an assertion message.
 */
internal class CieloCredentials(
    val clientId: String,
    val accessToken: String,
) {
    override fun toString(): String = "CieloCredentials(<redacted>)"
}
