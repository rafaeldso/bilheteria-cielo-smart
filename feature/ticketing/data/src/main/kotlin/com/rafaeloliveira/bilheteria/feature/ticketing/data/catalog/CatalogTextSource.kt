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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.catalog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Where the catalog JSON comes from. Abstracted so the repository is testable without Android. */
internal fun interface CatalogTextSource {
    /** Blocking read of the catalog text; throws [java.io.IOException] if missing. Call off the main thread. */
    fun readText(): String
}

/** Reads `assets/events.json` from the APK. */
internal class AssetCatalogTextSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : CatalogTextSource {
        override fun readText(): String = context.assets.open(ASSET_NAME).use { it.readBytes().decodeToString() }

        private companion object {
            const val ASSET_NAME = "events.json"
        }
    }
