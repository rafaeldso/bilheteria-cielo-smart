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
package com.rafaeloliveira.bilheteria.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icons of [DsStatusBadge], one per [DsStatusBadgeVariant]. The project has no Material Icons dependency,
 * so they are 24dp vectors built from Material path data (same approach as [ComponentIcons]).
 */
internal object StatusBadgeIcons {
    /** Check mark of [DsStatusBadgeVariant.Success]. */
    val Success: ImageVector by lazy {
        icon(name = "StatusSuccess", path = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    }

    /** Cross of [DsStatusBadgeVariant.Error]. */
    val Error: ImageVector by lazy {
        icon(
            name = "StatusError",
            path =
                "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 " +
                    "17.59 19 19 17.59 13.41 12z",
        )
    }

    /** Circle with a slash of [DsStatusBadgeVariant.Neutral]. */
    val Neutral: ImageVector by lazy {
        icon(
            name = "StatusNeutral",
            path =
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM4 12c0-4.42 3.58-8 8-8 " +
                    "1.85 0 3.55.63 4.9 1.69L5.69 16.9C4.63 15.55 4 13.85 4 12zm8 8c-1.85 0-3.55-.63-4.9-1.69" +
                    "L18.31 7.1C19.37 8.45 20 10.15 20 12c0 4.42-3.58 8-8 8z",
        )
    }

    /** Triangle with an exclamation mark of [DsStatusBadgeVariant.Warning]. */
    val Warning: ImageVector by lazy {
        icon(name = "StatusWarning", path = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z")
    }

    private const val ICON_VIEWPORT = 24f

    private fun icon(
        name: String,
        path: String,
    ): ImageVector =
        ImageVector
            .Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = ICON_VIEWPORT,
                viewportHeight = ICON_VIEWPORT,
            ).addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = SolidColor(Color.Black),
            ).build()
}
