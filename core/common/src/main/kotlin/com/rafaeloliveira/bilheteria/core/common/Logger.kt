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
package com.rafaeloliveira.bilheteria.core.common

/**
 * The only logging entry point (ADR-0007). Messages describe events and correlation IDs only:
 * never card data (PAN, CVV, track) or PII (security.md).
 */
interface Logger {
    fun info(message: String)

    fun warn(
        message: String,
        throwable: Throwable? = null,
    )

    fun error(
        message: String,
        throwable: Throwable? = null,
    )
}
