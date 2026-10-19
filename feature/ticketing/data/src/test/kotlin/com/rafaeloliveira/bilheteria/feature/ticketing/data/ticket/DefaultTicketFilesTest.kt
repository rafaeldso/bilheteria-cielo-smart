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
import androidx.core.content.FileProvider
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.data.purchase.RecordingLogger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Share images on Robolectric with real graphics: files, URIs, cleanup and failures (BR-41 to BR-43). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DefaultTicketFilesTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC)
    private val reference = "91ccb5d9-ccf6-4471-bfe2-413d5bc48637"
    private val set =
        TicketSet(
            purchaseId = reference,
            eventName = "Noite de Jazz no Parque",
            eventStartsAt = Instant.parse("2026-11-15T23:00:00Z"),
            venue = "Concha Acústica Municipal",
            tickets = (1..2).map { Ticket(it, 2, TicketCode("bilheteria:1:$reference:$it")) },
        )
    private val sharedDir = File(application.filesDir, DefaultTicketFiles.DIRECTORY)
    private val encoder = ZxingQrEncoder()
    private val printImages = CountingPrintImageStore()
    private val logger = RecordingLogger()

    /** `FileProvider` caches its roots per authority in a static map, but each test has its own `filesDir`. */
    @Before
    fun resetFileProviderCache() {
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
    }

    private fun files(qrEncoder: com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder = encoder) =
        DefaultTicketFiles(
            context = application,
            qrEncoder = qrEncoder,
            renderer = TicketImageRenderer(application, clock),
            printImages = printImages,
            clock = clock,
            ioDispatcher = Dispatchers.Unconfined,
            logger = logger,
        )

    private fun prepare(files: DefaultTicketFiles = files()) = runBlocking { files.prepareShare(set) }

    @Test
    fun `AC-41 - dado compra de 2 ingressos quando preparar entao uma imagem PNG de 1080 px por ingresso`() {
        val bundle = (prepare() as DataResult.Success).value

        assertEquals(2, bundle.uris.size)
        val images = sharedDir.listFiles().orEmpty()
        assertEquals(2, images.size)
        images.forEach { file ->
            val bitmap = BitmapFactory.decodeFile(file.path)
            assertNotNull(bitmap)
            assertEquals(TicketImageRenderer.SHARE_WIDTH_PX, bitmap.width)
        }
    }

    @Test
    fun `AC-42 - dado ingressos preparados quando inspecionar entao nao vazam a reference`() {
        val result = prepare()
        assertTrue(result is DataResult.Success, logger.warnings.toString())
        val bundle = (result as DataResult.Success).value

        assertTrue(bundle.uris.all { it.startsWith("content://${application.packageName}.tickets.fileprovider/") })
        assertTrue(bundle.uris.none { it.contains(reference) })
        assertTrue(sharedDir.listFiles().orEmpty().none { it.name.contains(reference) })
        assertTrue(sharedDir.canonicalPath.startsWith(application.filesDir.canonicalPath), "So em filesDir")
    }

    @Test
    fun `AC-42 - dado o manifest do data quando ler entao provider fechado e sem permissao de armazenamento`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val provider = manifest.substringAfter("<provider").substringBefore("</provider>")

        assertTrue(provider.contains("TicketFileProvider"))
        assertTrue(provider.contains("android:exported=\"false\""))
        assertTrue(provider.contains("android:grantUriPermissions=\"true\""))
        assertFalse(manifest.contains("EXTERNAL_STORAGE"), "Nenhuma permissao de armazenamento")
        assertFalse(manifest.contains("android.permission.READ_MEDIA"), "Nenhuma permissao de midia")
    }

    @Test
    fun `AC-42 - dado o XML de caminhos quando ler entao so expoe shared_tickets`() {
        val xml = File("src/main/res/xml/ticket_file_paths.xml").readText()

        assertEquals(1, Regex("<files-path").findAll(xml).count())
        assertTrue(xml.contains("path=\"shared_tickets/\""))
        assertFalse(xml.contains("external-path") || xml.contains("cache-path") || xml.contains("root-path"))
    }

    @Test
    fun `AC-43 - dado que a codificacao do QR Code falha quando preparar entao erro de geracao`() {
        val failing =
            com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder {
                DataResult.Failure(TicketingError.TicketGenerationFailed)
            }

        val result = prepare(files(failing))

        assertEquals(DataResult.Failure(TicketingError.TicketGenerationFailed), result)
    }

    @Test
    fun `AC-43 - dado que nao ha como gravar no diretorio quando preparar entao erro de geracao`() {
        sharedDir.deleteRecursively()
        sharedDir.writeText("isto e um arquivo, nao um diretorio")

        val result = prepare()

        assertEquals(DataResult.Failure(TicketingError.TicketGenerationFailed), result)
        sharedDir.delete()
    }

    @Test
    fun `AC-42 - dado imagem antiga quando preparar um novo compartilhamento entao a antiga e apagada e a nova fica`() {
        sharedDir.mkdirs()
        val old =
            File(sharedDir, "old.png").also {
                it.writeText("x")
                it.setLastModified(clock.millis() - TWO_HOURS)
            }
        val recent =
            File(sharedDir, "recent.png").also {
                it.writeText("x")
                it.setLastModified(clock.millis() - ONE_MINUTE)
            }

        prepare()

        assertFalse(old.exists())
        assertTrue(recent.exists())
    }

    @Test
    fun `AC-42 - dado imagem antiga quando o app abrir entao a limpeza apaga so as antigas`() {
        sharedDir.mkdirs()
        val old =
            File(sharedDir, "old.png").also {
                it.writeText("x")
                it.setLastModified(clock.millis() - TWO_HOURS)
            }
        val recent =
            File(sharedDir, "recent.png").also {
                it.writeText("x")
                it.setLastModified(clock.millis() - ONE_MINUTE)
            }

        runBlocking { files().sweepStale() }
        assertEquals(1, printImages.sweeps)

        assertFalse(old.exists())
        assertTrue(recent.exists())
    }

    private companion object {
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
        const val ONE_MINUTE = 60 * 1000L
    }
}

private class CountingPrintImageStore : PrintImageStore {
    var sweeps = 0
        private set

    override suspend fun write(
        set: TicketSet,
        ticket: Ticket,
    ): DataResult<String> = DataResult.Failure(TicketingError.TicketGenerationFailed)

    override fun delete(path: String) = Unit

    override fun sweepStale() {
        sweeps++
    }
}
