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

import android.content.res.Resources
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.toReceiptDateTime
import java.time.Instant
import java.time.ZoneId

/**
 * "15/11/2026 às 20:00": the absolute date and time of the receipt, outside composition (for a text that leaves the
 * app, such as the share text: whoever reads it later needs the real day, not "hoje").
 */
internal fun Instant.toReceiptDateText(
    zone: ZoneId,
    resources: Resources,
): String {
    val parts = toReceiptDateTime(zone)
    return resources.getString(R.string.ticketing_tickets_share_datetime, parts.date, parts.time)
}
