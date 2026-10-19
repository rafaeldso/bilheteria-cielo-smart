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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.ticket

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketWithQr
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Composition of the ticket image on Robolectric with real (native) graphics, so pixels can be decoded. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TicketImageRendererTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val renderer =
        TicketImageRenderer(application, Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC))
    private val code = TicketCode("bilheteria:1:91ccb5d9-ccf6-4471-bfe2-413d5bc48637:2")
    private val set =
        TicketSet(
            purchaseId = "p1",
            eventName = "Noite de Jazz no Parque",
            eventStartsAt = Instant.parse("2026-11-15T23:00:00Z"),
            venue = "Concha Acústica Municipal",
            tickets = listOf(Ticket(2, 3, code)),
        )
    private val item = TicketWithQr(set.tickets.first(), (ZxingQrEncoder().encode(code) as DataResult.Success).value)

    @Test
    fun `AC-38 - dado ingresso quando compor para a impressora entao a imagem tem 384 px de largura`() {
        val png = renderer.renderPng(set, item, TicketImageRenderer.PRINT_WIDTH_PX)

        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size)

        assertNotNull(bitmap)
        assertEquals(TicketImageRenderer.PRINT_WIDTH_PX, bitmap.width)
    }

    @Test
    fun `AC-29 - dado ingresso quando compor a imagem entao o QR Code dentro dela e decodificado igual ao codigo`() {
        for (width in listOf(TicketImageRenderer.PRINT_WIDTH_PX, TicketImageRenderer.SHARE_WIDTH_PX)) {
            val bitmap: Bitmap = renderer.render(set, item, width)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            assertEquals(code.value, decodeQr(pixels, bitmap.width, bitmap.height), "imagem de $width px")
        }
    }
}
