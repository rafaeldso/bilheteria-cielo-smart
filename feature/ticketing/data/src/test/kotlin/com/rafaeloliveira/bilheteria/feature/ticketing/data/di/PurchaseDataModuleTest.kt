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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.di

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PurchaseDataModuleTest {
    private val generator = PurchaseDataModule.provideReferenceGenerator()

    @Test
    fun `AC-07 - dado duas compras quando gerar a referencia entao sao UUID v4 distintos`() {
        val references = List(100) { generator.next().value }

        assertEquals(100, references.toSet().size)
        references.forEach { assertEquals(4, UUID.fromString(it).version()) }
    }

    @Test
    fun `AC-07 - dado referencia gerada quando enviar a Cielo entao tem o formato textual do UUID`() {
        assertTrue(UUID_PATTERN.matches(generator.next().value))
    }

    private companion object {
        val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }
}
