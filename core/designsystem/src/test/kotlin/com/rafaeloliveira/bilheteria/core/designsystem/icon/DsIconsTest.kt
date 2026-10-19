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

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DsIconsTest {
    private val all: Map<String, ImageVector> =
        mapOf(
            "ArrowBack" to DsIcons.ArrowBack,
            "Add" to DsIcons.Add,
            "Remove" to DsIcons.Remove,
            "Check" to DsIcons.Check,
            "CheckCircle" to DsIcons.CheckCircle,
            "Block" to DsIcons.Block,
            "Cancel" to DsIcons.Cancel,
            "Error" to DsIcons.Error,
            "Warning" to DsIcons.Warning,
            "Refresh" to DsIcons.Refresh,
            "Sync" to DsIcons.Sync,
            "HourglassTop" to DsIcons.HourglassTop,
            "Inbox" to DsIcons.Inbox,
            "Event" to DsIcons.Event,
            "EventBusy" to DsIcons.EventBusy,
            "History" to DsIcons.History,
            "ReceiptLong" to DsIcons.ReceiptLong,
            "ConfirmationNumber" to DsIcons.ConfirmationNumber,
            "Print" to DsIcons.Print,
            "Share" to DsIcons.Share,
            "PointOfSale" to DsIcons.PointOfSale,
            "AddShoppingCart" to DsIcons.AddShoppingCart,
            "Lock" to DsIcons.Lock,
        )

    @Test
    fun `dado cada icone do design system quando inspecionar entao e um vetor de 24dp em viewport 24 com caminho`() {
        all.forEach { (name, icon) ->
            assertEquals(name, icon.name, "$name name")
            assertEquals(24.dp, icon.defaultWidth, "$name width")
            assertEquals(24.dp, icon.defaultHeight, "$name height")
            assertEquals(24f, icon.viewportWidth, "$name viewportWidth")
            assertEquals(24f, icon.viewportHeight, "$name viewportHeight")
            assertTrue(icon.root.size > 0, "$name has no path")
        }
    }

    @Test
    fun `dado os icones direcionais quando inspecionar entao so a seta de voltar espelha em RTL`() {
        assertTrue(DsIcons.ArrowBack.autoMirror)
        all.filterKeys { it != "ArrowBack" }.forEach { (name, icon) ->
            assertFalse(icon.autoMirror, "$name must not mirror")
        }
    }
}
