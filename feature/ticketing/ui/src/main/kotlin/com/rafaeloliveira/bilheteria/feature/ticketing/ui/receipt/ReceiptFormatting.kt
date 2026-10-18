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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.PtBr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val DATE_PATTERN = "dd/MM/yyyy"
private const val TIME_PATTERN = "HH:mm"

/** Absolute date and time of an instant, in two parts so the localized "às" stays in a string resource. */
internal data class ReceiptDateTime(
    val date: String,
    val time: String,
)

/**
 * Absolute `dd/MM/yyyy` and `HH:mm` in [zone] (BR-16, ui-spec 4.4): the receipt can be reopened days later, so
 * "hoje"/"ontem" would be wrong. The zone is explicit (no hidden default) and the locale is pt-BR.
 */
internal fun Instant.toReceiptDateTime(zone: ZoneId): ReceiptDateTime =
    ReceiptDateTime(
        date = DateTimeFormatter.ofPattern(DATE_PATTERN, PtBr).withZone(zone).format(this),
        time = DateTimeFormatter.ofPattern(TIME_PATTERN, PtBr).withZone(zone).format(this),
    )
