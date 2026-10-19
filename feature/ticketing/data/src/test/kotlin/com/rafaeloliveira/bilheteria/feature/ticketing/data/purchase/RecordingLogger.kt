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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase

import com.rafaeloliveira.bilheteria.core.common.Logger

/** Keeps the messages so tests can check what was logged (and that it carries no sensitive data). */
internal class RecordingLogger : Logger {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val infos = mutableListOf<String>()

    override fun info(message: String) {
        infos += message
    }

    override fun warn(
        message: String,
        throwable: Throwable?,
    ) {
        warnings += message
    }

    override fun error(
        message: String,
        throwable: Throwable?,
    ) {
        errors += message
    }
}
