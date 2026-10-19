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

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.core.common.IoDispatcher
import com.rafaeloliveira.bilheteria.core.common.Logger
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Ticket
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketWithQr
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * [PrintImageStore] over `MediaStore.Images` (plan §4, risk R1): the Cielo app reads the image by path, so it has to be
 * in shared storage. From API 29 an app inserts and deletes its own entries with **no permission**; below it, the
 * device is treated as unable to print (`TerminalAppUnavailable`), so this app never asks for storage permissions.
 * The entry has a random name (never the `reference`), lives under `Pictures/Bilheteria` and is deleted by the printer
 * when the step ends. Paths and contents are never logged.
 */
internal class MediaStorePrintImageStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val qrEncoder: QrEncoder,
        private val renderer: TicketImageRenderer,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val logger: Logger,
    ) : PrintImageStore {
        private val resolver: ContentResolver get() = context.contentResolver

        override suspend fun write(
            set: TicketSet,
            ticket: Ticket,
        ): DataResult<String> =
            withContext(ioDispatcher) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    DataResult.Failure(TicketingError.TerminalAppUnavailable)
                } else {
                    insert(set, ticket)
                }
            }

        private fun insert(
            set: TicketSet,
            ticket: Ticket,
        ): DataResult<String> =
            try {
                val qr =
                    when (val encoded = qrEncoder.encode(ticket.code)) {
                        is DataResult.Success -> encoded.value
                        is DataResult.Failure -> throw IOException("QR Code not encoded")
                    }
                val png = renderer.renderPng(set, TicketWithQr(ticket, qr), TicketImageRenderer.PRINT_WIDTH_PX)
                val uri = resolver.insert(IMAGES, pendingEntry()) ?: throw IOException("No MediaStore entry")
                DataResult.Success(publish(uri, png))
            } catch (expected: IOException) {
                failure("print_image_io")
            } catch (expected: SecurityException) {
                failure("print_image_denied")
            } catch (expected: IllegalStateException) {
                failure("print_image_png_failed")
            }

        private fun pendingEntry() =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${UUID.randomUUID()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

        /**
         * Writes [png] into [uri], publishes it (no longer pending) and returns its absolute path. The row is deleted
         * on
         * any failure, so a pending entry holding the QR Code never stays in the gallery index.
         */
        private fun publish(
            uri: Uri,
            png: ByteArray,
        ): String {
            var published = false
            try {
                resolver.openOutputStream(uri)?.use { it.write(png) } ?: throw IOException("No stream")
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                return pathOf(uri).also { published = true }
            } finally {
                if (!published) resolver.delete(uri, null, null)
            }
        }

        private fun pathOf(uri: Uri): String =
            resolver.query(uri, arrayOf(PATH_COLUMN), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: throw IOException("No path")

        override fun delete(path: String) {
            try {
                resolver.delete(IMAGES, "$PATH_COLUMN=?", arrayOf(path))
            } catch (expected: SecurityException) {
                logger.warn("print_image_not_deleted")
            }
        }

        override fun sweepStale() {
            try {
                resolver.delete(IMAGES, "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf("$RELATIVE_PATH%"))
            } catch (expected: SecurityException) {
                logger.warn("print_image_sweep_denied")
            }
        }

        private fun failure(reason: String): DataResult<String> {
            logger.warn(reason)
            return DataResult.Failure(TicketingError.TicketGenerationFailed)
        }

        private companion object {
            const val RELATIVE_PATH = "Pictures/Bilheteria"
            val IMAGES: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            const val PATH_COLUMN = MediaStore.MediaColumns.DATA
        }
    }
