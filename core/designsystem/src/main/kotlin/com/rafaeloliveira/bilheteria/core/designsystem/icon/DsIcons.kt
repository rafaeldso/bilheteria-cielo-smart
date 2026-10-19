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
package com.rafaeloliveira.bilheteria.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icons of the design system: the ones the screens of the ticketing feature use (ui-spec SCR-01 to SCR-04).
 * The project has no Material Icons dependency, so each one is a 24dp vector built once from Material path
 * data. They are decorative by default: pass them to a component that supplies the spoken label
 * (`DsIconButton`, `DsButton`, `DsStatusBadge`) and never rely on the glyph alone.
 */
object DsIcons {
    /** Back arrow of the top app bar; mirrors in right-to-left layouts. */
    val ArrowBack: ImageVector by lazy {
        materialIcon(
            name = "ArrowBack",
            path = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
            autoMirror = true,
        )
    }

    /** Plus sign: increase a quantity. */
    val Add: ImageVector by lazy { materialIcon(name = "Add", path = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }

    /** Minus sign: decrease a quantity. */
    val Remove: ImageVector by lazy { materialIcon(name = "Remove", path = "M19 13H5v-2h14v2z") }

    /** Check mark: a selected filter chip. */
    val Check: ImageVector by lazy {
        materialIcon(name = "Check", path = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    }

    /** Check in a circle: success, an approved payment. */
    val CheckCircle: ImageVector by lazy {
        materialIcon(
            name = "CheckCircle",
            path =
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 " +
                    "14.17l7.59-7.59L19 8l-9 9z",
        )
    }

    /** Circle with a slash: a denied payment. */
    val Block: ImageVector by lazy {
        materialIcon(
            name = "Block",
            path =
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM4 12c0-4.42 3.58-8 8-8 " +
                    "1.85 0 3.55.63 4.9 1.69L5.69 16.9C4.63 15.55 4 13.85 4 12zm8 8c-1.85 0-3.55-.63-4.9-1.69" +
                    "L18.31 7.1C19.37 8.45 20 10.15 20 12c0 4.42-3.58 8-8 8z",
        )
    }

    /** Cross in a circle: something cancelled, neutral outcome. */
    val Cancel: ImageVector by lazy {
        materialIcon(
            name = "Cancel",
            path =
                "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 " +
                    "13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z",
        )
    }

    /** Exclamation mark in a circle: a failure. */
    val Error: ImageVector by lazy {
        materialIcon(
            name = "Error",
            path =
                "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2z" +
                    "m0-4h-2V7h2v6z",
        )
    }

    /** Exclamation mark in a triangle: a warning that needs attention but is not a failure. */
    val Warning: ImageVector by lazy {
        materialIcon(name = "Warning", path = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z")
    }

    /** Circular arrow: try again. */
    val Refresh: ImageVector by lazy {
        materialIcon(
            name = "Refresh",
            path =
                "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 " +
                    "7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 " +
                    "1.78L13 11h7V4l-2.35 2.35z",
        )
    }

    /** Two circular arrows: syncing or confirming with a remote service. */
    val Sync: ImageVector by lazy {
        materialIcon(
            name = "Sync",
            path =
                "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 " +
                    "20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 " +
                    "8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z",
        )
    }

    /** Hourglass: waiting for something to finish. */
    val HourglassTop: ImageVector by lazy {
        materialIcon(
            name = "HourglassTop",
            path =
                "M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2H6z" +
                    "m10 14.5V20H8v-3.5l4-4 4 4zm-4-5l-4-4V4h8v3.5l-4 4z",
        )
    }

    /** Tray: nothing here yet (generic empty state). */
    val Inbox: ImageVector by lazy {
        materialIcon(
            name = "Inbox",
            path =
                "M19 3H4.99c-1.11 0-1.98.89-1.98 2L3 19c0 1.1.88 2 1.99 2H19c1.1 0 2-.9 2-2V5c0-1.11-.9-2-2-2z" +
                    "m0 12h-4c0 1.66-1.35 3-3 3s-3-1.34-3-3H4.99V5H19v10z",
        )
    }

    /** Calendar: an event. */
    val Event: ImageVector by lazy {
        materialIcon(
            name = "Event",
            path =
                "M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 " +
                    "2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z",
        )
    }

    /** Calendar with a cross: the events could not be loaded. */
    val EventBusy: ImageVector by lazy {
        materialIcon(
            name = "EventBusy",
            path =
                "M9.31 17l2.44-2.44L14.19 17l1.06-1.06-2.44-2.44 2.44-2.44L14.19 10l-2.44 2.44L9.31 10l" +
                    "-1.06 1.06 2.44 2.44-2.44 2.44L9.31 17zM19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 " +
                    "2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z",
        )
    }

    /** Clockwise arrow around a clock hand: the purchase history. */
    val History: ImageVector by lazy {
        materialIcon(
            name = "History",
            path =
                "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 " +
                    "7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 " +
                    "9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z",
        )
    }

    /** Long receipt: the purchase history and its empty state. */
    val ReceiptLong: ImageVector by lazy {
        materialIcon(
            name = "ReceiptLong",
            path =
                "M19.5 3.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2v14H3v3c0 1.66 1.34 " +
                    "3 3 3h12c1.66 0 3-1.34 3-3V2l-1.5 1.5zM15 20H6c-.55 0-1-.45-1-1v-1h10v2zm4-1c0 .55-.45 " +
                    "1-1 1s-1-.45-1-1v-3H8V5h11v14zM9 7h6v2H9zm7 0h2v2h-2zm-7 3h6v2H9zm7 0h2v2h-2z",
        )
    }

    /** Ticket: an event for sale. */
    val ConfirmationNumber: ImageVector by lazy {
        materialIcon(
            name = "ConfirmationNumber",
            path =
                "M22 10V6c0-1.1-.9-2-2-2H4c-1.1 0-1.99.9-1.99 2v4c1.1 0 1.99.9 1.99 2s-.89 2-2 2v4c0 1.1.9 " +
                    "2 2 2h16c1.1 0 2-.9 2-2v-4c-1.1 0-2-.9-2-2s.9-2 2-2zm-9 7.5h-2v-2h2v2zm0-4.5h-2v-2h2v2z" +
                    "m0-4.5h-2v-2h2v2z",
        )
    }

    /** Printer: print the tickets on the terminal. */
    val Print: ImageVector by lazy {
        materialIcon(
            name = "Print",
            path =
                "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 " +
                    "0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z",
        )
    }

    /** Share: open the system share sheet. */
    val Share: ImageVector by lazy {
        materialIcon(
            name = "Share",
            path =
                "M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c" +
                    ".54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 " +
                    "9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 " +
                    "4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31" +
                    "-2.92-2.92-2.92z",
        )
    }

    /** Card terminal: pay on the terminal. */
    val PointOfSale: ImageVector by lazy {
        materialIcon(
            name = "PointOfSale",
            path =
                "M17 2H7c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 4H7V4h10v2z" +
                    "m3 16H4c-1.1 0-2-.9-2-2v-1h20v1c0 1.1-.9 2-2 2zm-1.47-11.81C18.24 9.47 17.63 9 16.88 " +
                    "9H7.12c-.75 0-1.36.47-1.65 1.19L2 18h20l-3.47-7.81zM9.5 16h-1v-1h1v1zm0-2h-1v-1h1v1zm0-2" +
                    "h-1v-1h1v1zm2.5 4h-1v-1h1v1zm0-2h-1v-1h1v1zm0-2h-1v-1h1v1zm2.5 4h-1v-1h1v1zm0-2h-1v-1h1" +
                    "v1zm0-2h-1v-1h1v1zm2.5 4h-1v-1h1v1zm0-2h-1v-1h1v1zm0-2h-1v-1h1v1z",
        )
    }

    /** Cart with a plus: start a new sale. */
    val AddShoppingCart: ImageVector by lazy {
        materialIcon(
            name = "AddShoppingCart",
            path =
                "M11 9h2V6h3V4h-3V1h-2v3H8v2h3v3zm-4 9c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2z" +
                    "m10 0c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2zm-9.83-3.25l.03-.12.9-1.63h7.45" +
                    "c.75 0 1.41-.41 1.75-1.03l3.86-7.01L19.42 4h-.01l-1.1 2-2.76 5H8.53l-.13-.27L6.16 6l-.95" +
                    "-2-.94-2H1v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.13 0" +
                    "-.25-.11-.25-.25z",
        )
    }

    /** Padlock: an authentication problem. */
    val Lock: ImageVector by lazy {
        materialIcon(
            name = "Lock",
            path =
                "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 " +
                    "2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6" +
                    "c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z",
        )
    }
}

private const val ICON_VIEWPORT = 24f

/** Builds a 24dp, 24-unit viewport vector from Material path data, painted in black so callers tint it. */
internal fun materialIcon(
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
