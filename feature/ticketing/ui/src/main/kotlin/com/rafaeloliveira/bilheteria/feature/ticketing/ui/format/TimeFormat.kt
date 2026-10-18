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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.format

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val Time24Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", PtBr)

/** 24-hour clock time of [dateTime] in its own zone, "09:05": the single place that formats a time of day. */
internal fun formatTime24(dateTime: ZonedDateTime): String = Time24Formatter.format(dateTime)
