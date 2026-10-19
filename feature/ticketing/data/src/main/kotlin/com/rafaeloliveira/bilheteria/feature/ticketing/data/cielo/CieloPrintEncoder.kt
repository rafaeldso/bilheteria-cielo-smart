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

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.io.encoding.Base64

/**
 * Builds the `lio://print` URI of one ticket image (spec 002, research.md §1): `PRINT_IMAGE` with the file path,
 * centered,
 * and **no credentials** (the spike confirmed the emulator does not ask for them). Base64 and percent-encoding follow
 * [CieloRequestEncoder]. The path is never logged.
 */
internal class CieloPrintEncoder
    @Inject
    constructor() {
        /** URI that prints the image at [imagePath] (an absolute path the Cielo app can read). */
        fun encodeImage(imagePath: String): String {
            val request =
                PrintRequestDto(operation = OPERATION_IMAGE, styles = listOf(CENTERED), value = listOf(imagePath))
            val base64 = Base64.Default.encode(json.encodeToString(request).encodeToByteArray())
            val query = base64.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D")
            return "lio://print?request=$query&urlCallback=order://response"
        }

        private companion object {
            const val OPERATION_IMAGE = "PRINT_IMAGE"
            val CENTERED = mapOf("key_attributes_align" to 0)
            val json = Json { encodeDefaults = true }
        }
    }

@Serializable
internal data class PrintRequestDto(
    val operation: String,
    val styles: List<Map<String, Int>>,
    val value: List<String>,
)
