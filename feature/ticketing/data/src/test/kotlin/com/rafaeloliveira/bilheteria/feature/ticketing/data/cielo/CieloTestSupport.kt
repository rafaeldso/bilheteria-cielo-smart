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

import kotlin.io.encoding.Base64

/** Fictitious credentials: never real tokens in tests. */
internal const val TEST_CLIENT_ID = "fake-client"
internal const val TEST_ACCESS_TOKEN = "fake-token"

/** Reads a Cielo documentation fixture from `src/test/resources/cielo/`. */
internal fun fixture(name: String): String {
    val stream =
        checkNotNull(object {}.javaClass.getResourceAsStream("/cielo/$name")) { "Fixture not found: $name" }
    return stream.use { it.readBytes().decodeToString() }
}

/**
 * Encodes [json] the way the Cielo app does: Base64 with a line break every 76 characters (`Base64.DEFAULT`),
 * the format of the real `response` parameter (research.md §13).
 */
internal fun cieloBase64(json: String): String =
    Base64.Default
        .encode(json.encodeToByteArray())
        .chunked(MIME_LINE_LENGTH)
        .joinToString("\n")

private const val MIME_LINE_LENGTH = 76
