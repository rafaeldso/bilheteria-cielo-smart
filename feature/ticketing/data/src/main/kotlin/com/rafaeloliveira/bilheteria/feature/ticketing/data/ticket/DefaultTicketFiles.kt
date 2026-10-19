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

import android.content.Context
import androidx.core.content.FileProvider
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.common.IoDispatcher
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketWithQr
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.ShareBundle
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.TicketFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * [TicketFiles] over the private `files/shared_tickets/` directory (BR-41, BR-42, plan §4): one PNG per ticket with a
 * random name (never the `reference`), served as `content://` URIs by [TicketFileProvider]. The images are never
 * written to external storage, so no storage permission is needed. Files older than [MAX_AGE_MILLIS] are deleted each
 * time a share is prepared and when the tickets screen opens ([sweepStale]). Paths, URIs and contents are never logged.
 */
internal class DefaultTicketFiles
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val qrEncoder: QrEncoder,
        private val renderer: TicketImageRenderer,
        private val printImages: PrintImageStore,
        private val clock: Clock,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val logger: Logger,
    ) : TicketFiles {
        override suspend fun prepareShare(set: TicketSet): DataResult<ShareBundle> =
            withContext(ioDispatcher) {
                try {
                    val dir = sharedDir()
                    deleteStale(dir)
                    DataResult.Success(ShareBundle(set.tickets.map { uriOf(imageOf(set, it, dir)) }))
                } catch (expected: IOException) {
                    failure("share_files_io")
                } catch (expected: IllegalArgumentException) {
                    failure("share_files_invalid") // FileProvider rejects a path outside its configured roots
                } catch (expected: IllegalStateException) {
                    failure("share_png_failed")
                }
            }

        /** Deletes the shared images older than [MAX_AGE_MILLIS] and the print images an earlier process left. */
        override suspend fun sweepStale() {
            withContext(ioDispatcher) {
                deleteStale(File(context.filesDir, DIRECTORY))
                printImages.sweepStale()
            }
        }

        private fun imageOf(
            set: TicketSet,
            ticket: Ticket,
            dir: File,
        ): File {
            val qr =
                when (val encoded = qrEncoder.encode(ticket.code)) {
                    is DataResult.Success -> encoded.value
                    is DataResult.Failure -> throw IOException("QR Code not encoded")
                }
            val png = renderer.renderPng(set, TicketWithQr(ticket, qr), TicketImageRenderer.SHARE_WIDTH_PX)
            val file = File(dir, "${UUID.randomUUID()}.png")
            try {
                file.writeBytes(png)
            } catch (expected: IOException) {
                file.delete() // a half-written image must not stay behind
                throw expected
            }
            return file
        }

        private fun uriOf(file: File): String = FileProvider.getUriForFile(context, authority(), file).toString()

        private fun authority(): String = "${context.packageName}.$AUTHORITY_SUFFIX"

        private fun sharedDir(): File =
            File(context.filesDir, DIRECTORY).also {
                if (!it.isDirectory &&
                    !it.mkdirs()
                ) {
                    throw IOException("No directory")
                }
            }

        private fun deleteStale(dir: File) {
            val limit = clock.millis() - MAX_AGE_MILLIS
            dir.listFiles()?.filter { it.isFile && it.lastModified() < limit }?.forEach { it.delete() }
        }

        private fun failure(reason: String): DataResult<ShareBundle> {
            logger.warn(reason)
            return DataResult.Failure(TicketingError.TicketGenerationFailed)
        }

        internal companion object {
            const val DIRECTORY = "shared_tickets"
            const val AUTHORITY_SUFFIX = "tickets.fileprovider"
            const val MAX_AGE_MILLIS = 60 * 60 * 1000L
        }
    }
