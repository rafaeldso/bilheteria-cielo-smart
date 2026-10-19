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
import android.graphics.BitmapFactory
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase.RecordingLogger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** The device rule of the print image: below API 29 there is no storage permission to rely on, so it cannot print. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaStorePrintImageStoreTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val ticket = Ticket(1, 1, TicketCode("bilheteria:1:ref:1"))
    private val set =
        TicketSet(
            "p1",
            "Noite de Jazz no Parque",
            Instant.parse("2026-11-15T23:00:00Z"),
            "Concha Acústica",
            listOf(ticket),
        )

    private fun store() =
        MediaStorePrintImageStore(
            context = application,
            qrEncoder = ZxingQrEncoder(),
            renderer =
                TicketImageRenderer(
                    application,
                    Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC),
                ),
            ioDispatcher = Dispatchers.Unconfined,
            logger = RecordingLogger(),
        )

    @Test
    @Config(sdk = [28])
    fun `AC-40 - dado aparelho com API 28 quando gravar a imagem de impressao entao terminal indisponivel`() {
        val result = runBlocking { store().write(set, ticket) }

        assertEquals(DataResult.Failure(TicketingError.TerminalAppUnavailable), result)
    }

    @Before
    fun registerMedia() {
        FakeMediaProvider.reset()
        Robolectric.setupContentProvider(FakeMediaProvider::class.java, "media")
    }

    @After
    fun clearMedia() = FakeMediaProvider.reset()

    @Test
    fun `AC-38 - dado ingresso quando gravar a imagem de impressao entao o caminho abre um PNG de 384 px publicado`() {
        val path = (runBlocking { store().write(set, ticket) } as DataResult.Success).value

        val row = FakeMediaProvider.rows.single()
        assertEquals(path, row.file.path)
        assertEquals(0, row.values.getAsInteger("is_pending"), "published, no longer pending")
        assertEquals("Pictures/Bilheteria", row.values.getAsString("relative_path"))
        assertEquals(TicketImageRenderer.PRINT_WIDTH_PX, BitmapFactory.decodeFile(path).width)
        assertTrue(!row.values.getAsString("_display_name").contains("ref"), "random name, never the reference")
    }

    @Test
    fun `AC-38 - dado imagem gravada quando apagar pelo caminho entao a linha e o arquivo somem`() {
        val path = (runBlocking { store().write(set, ticket) } as DataResult.Success).value

        store().delete(path)

        assertTrue(FakeMediaProvider.rows.isEmpty())
    }

    @Test
    fun `AC-42 - dado imagens que sobraram quando limpar entao todas as de Pictures Bilheteria somem`() {
        runBlocking {
            store().write(set, ticket)
            store().write(set, ticket)
        }
        assertEquals(2, FakeMediaProvider.rows.size)

        store().sweepStale()

        assertTrue(FakeMediaProvider.rows.isEmpty())
    }

    @Test
    fun `AC-40 - dado que o MediaStore nao cria a linha quando gravar entao falha de geracao`() {
        FakeMediaProvider.failInsert = true

        val result = runBlocking { store().write(set, ticket) }

        assertEquals(DataResult.Failure(TicketingError.TicketGenerationFailed), result)
    }

    @Test
    fun `AC-40 - dado que nao da para abrir o arquivo quando gravar entao a linha pendente e apagada`() {
        FakeMediaProvider.failOpen = true

        val result = runBlocking { store().write(set, ticket) }

        assertEquals(DataResult.Failure(TicketingError.TicketGenerationFailed), result)
        assertTrue(FakeMediaProvider.rows.isEmpty(), "a pending row holding the QR Code must not stay")
        assertNotNull(result)
    }
}
