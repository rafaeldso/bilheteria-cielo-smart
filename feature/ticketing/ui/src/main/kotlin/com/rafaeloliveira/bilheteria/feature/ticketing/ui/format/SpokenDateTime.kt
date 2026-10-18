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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private const val TIME_PATTERN = "HH:mm"

/**
 * Day, month and time of an instant to be read aloud as "15 de outubro às 09:00", so TalkBack does not read
 * "15/10" as "quinze barra dez". [monthName] is in full and pt-BR ("outubro").
 */
internal data class SpokenDateTime(
    val day: Int,
    val monthName: String,
    val time: String,
)

/** [SpokenDateTime] of this instant in [zone] (an explicit zone, never the device default). */
internal fun Instant.toSpokenDateTime(zone: ZoneId): SpokenDateTime {
    val local = atZone(zone)
    return SpokenDateTime(
        day = local.dayOfMonth,
        monthName = local.month.getDisplayName(TextStyle.FULL, PtBr),
        time = DateTimeFormatter.ofPattern(TIME_PATTERN, PtBr).format(local),
    )
}

@Composable
internal fun spokenDateTime(spoken: SpokenDateTime): String = LocalResources.current.spokenDateTimeText(spoken)

/** The resource lookup behind [spokenDateTime]; usable outside composition, e.g. inside a `remember`. */
internal fun Resources.spokenDateTimeText(spoken: SpokenDateTime): String =
    getString(R.string.ticketing_common_datetime_spoken, spoken.day, spoken.monthName, spoken.time)
