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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketFiles
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.FakeTicketPrinter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SweepTicketFilesUseCaseTest {
    private val files = FakeTicketFiles()
    private val printer = FakeTicketPrinter()

    @Test
    fun `AC-42 - dado nenhuma impressao em andamento quando abrir os ingressos entao limpa as imagens antigas`() =
        runTest {
            SweepTicketFilesUseCase(files, printer)()

            assertEquals(1, files.swept)
        }

    @Test
    fun `AC-42 - dado impressao em andamento quando limpar entao nao apaga a imagem em uso`() =
        runTest {
            printer.emit(PrintStatus.Printing("p1", printed = 0, total = 2))

            SweepTicketFilesUseCase(files, printer)()

            assertEquals(0, files.swept)
        }
}
