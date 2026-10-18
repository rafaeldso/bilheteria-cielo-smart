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
 * Icons the components draw themselves (the check of a selected chip) and the ones the previews use.
 * The project has no Material Icons dependency, so they are 24dp vectors built from Material path data.
 */
internal object ComponentIcons {
    /** Check mark shown on a selected [DsFilterChip]. */
    val Check: ImageVector by lazy {
        icon(name = "Check", path = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    }

    /** Back arrow; mirrors in right-to-left layouts. Used by previews. */
    val ArrowBack: ImageVector by lazy {
        icon(
            name = "ArrowBack",
            path = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
            autoMirror = true,
        )
    }

    /** Plus sign. Used by previews. */
    val Add: ImageVector by lazy { icon(name = "Add", path = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }

    /** Minus sign. Used by previews. */
    val Remove: ImageVector by lazy { icon(name = "Remove", path = "M19 13H5v-2h14v2z") }

    private const val ICON_VIEWPORT = 24f

    private fun icon(
        name: String,
        path: String,
        autoMirror: Boolean = false,
    ): ImageVector =
        ImageVector
            .Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = ICON_VIEWPORT,
                viewportHeight = ICON_VIEWPORT,
                autoMirror = autoMirror,
            ).addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = SolidColor(Color.Black),
            ).build()
}
