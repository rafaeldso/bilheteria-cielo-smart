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

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.error.TicketingError
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.TicketCode
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.ticket.QrEncoder
import javax.inject.Inject

/**
 * [QrEncoder] over ZXing core (spec 002, Q7): pure Java, no camera, no permission. Error correction level M and no
 * margin: the 4-module quiet zone belongs to the drawing ([QrRaster], `DsQrCode`), so the matrix is the bare code.
 * The content is never logged or put in an exception message (it holds the purchase `reference`).
 */
internal class ZxingQrEncoder
    @Inject
    constructor() : QrEncoder {
        override fun encode(code: TicketCode): DataResult<QrMatrix> =
            try {
                val bits = QRCodeWriter().encode(code.value, BarcodeFormat.QR_CODE, 0, 0, HINTS)
                val side = bits.width
                DataResult.Success(QrMatrix(side, BooleanArray(side * side) { bits.get(it % side, it / side) }))
            } catch (expected: WriterException) {
                DataResult.Failure(TicketingError.TicketGenerationFailed)
            } catch (expected: IllegalArgumentException) {
                DataResult.Failure(TicketingError.TicketGenerationFailed)
            }

        private companion object {
            val HINTS: Map<EncodeHintType, Any> =
                mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN to 0,
                )
        }
    }
