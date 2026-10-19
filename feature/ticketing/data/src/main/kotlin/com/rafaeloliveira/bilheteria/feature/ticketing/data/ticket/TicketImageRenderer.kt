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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.rafaeloliveira.bilheteria.feature.ticketing.data.R
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketSet
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketWithQr
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Composes the image of one ticket (BR-35, BR-38, BR-41): "Ingresso n de q", the event, date and venue, and the QR
 * Code, black on white. The same composition serves the printer ([PRINT_WIDTH_PX], 384 px as the Cielo
 * `PRINT_IMAGE` asks) and the share sheet ([SHARE_WIDTH_PX]). Only data the spec allows goes in: no transaction data,
 * no card data, no `reference` as text (it is inside the QR Code only). The date is absolute, as in the receipt.
 */
internal class TicketImageRenderer
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val clock: Clock,
    ) {
        /** PNG bytes of the ticket image with the given [widthPx]. */
        fun renderPng(
            set: TicketSet,
            item: TicketWithQr,
            widthPx: Int,
        ): ByteArray {
            val bitmap = render(set, item, widthPx)
            return ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)) { "PNG encoding failed" }
                bitmap.recycle()
                out.toByteArray()
            }
        }

        internal fun render(
            set: TicketSet,
            item: TicketWithQr,
            widthPx: Int,
        ): Bitmap {
            val margin = widthPx * MARGIN_PERCENT / PERCENT
            val textWidth = widthPx - 2 * margin
            val titleText = context.getString(R.string.ticket_image_title, item.ticket.ordinal, item.ticket.total)
            val title = layout(titleText, textWidth, widthPx, bold = true)
            val details = layout(detailsOf(set), textWidth, widthPx, bold = false)
            val qrSize = widthPx - 2 * margin
            val height = margin + title.height + margin / 2 + qrSize + margin / 2 + details.height + margin
            val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            draw(canvas, title, margin, margin)
            val qrTop = margin + title.height + margin / 2
            bitmap.setPixels(QrRaster.render(item.qr, qrSize), 0, qrSize, margin, qrTop, qrSize, qrSize)
            draw(canvas, details, margin, qrTop + qrSize + margin / 2)
            return bitmap
        }

        private fun detailsOf(set: TicketSet): String {
            val startsAt = DATE_FORMAT.format(set.eventStartsAt.atZone(zone()))
            return context.getString(R.string.ticket_image_details, set.eventName, startsAt, set.venue)
        }

        private fun zone(): ZoneId = clock.zone

        private fun layout(
            text: String,
            textWidth: Int,
            widthPx: Int,
            bold: Boolean,
        ): StaticLayout {
            val paint =
                TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = widthPx * (if (bold) TITLE_SIZE_PERCENT else BODY_SIZE_PERCENT) / PERCENT
                    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                }
            return StaticLayout.Builder
                .obtain(text, 0, text.length, paint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setMaxLines(MAX_LINES)
                .build()
        }

        private fun draw(
            canvas: Canvas,
            layout: StaticLayout,
            left: Int,
            top: Int,
        ) {
            canvas.save()
            canvas.translate(left.toFloat(), top.toFloat())
            layout.draw(canvas)
            canvas.restore()
        }

        internal companion object {
            const val PRINT_WIDTH_PX = 384
            const val SHARE_WIDTH_PX = 1080
            private const val MARGIN_PERCENT = 6
            private const val PERCENT = 100
            private const val TITLE_SIZE_PERCENT = 7f
            private const val BODY_SIZE_PERCENT = 4.5f
            private const val MAX_LINES = 6
            private const val PNG_QUALITY = 100
            private val DATE_FORMAT: DateTimeFormatter =
                DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm", Locale("pt", "BR"))
        }
    }
