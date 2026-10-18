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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.catalog

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.format.DateTimeParseException

class EventDtoMapperTest {
    private fun dto(
        id: String = "t-1",
        name: String = "Show de Teste",
        startsAt: String = "2026-11-14T20:00:00-03:00",
        venue: String = "Teatro de Teste",
        priceCents: Long = 8_000,
        totalTickets: Int = 10,
    ) = EventDto(id, name, startsAt, venue, priceCents, totalTickets)

    @Test
    fun `AC-01 - dado dto valido quando mapear entao preserva nome local preco e estoque do evento`() {
        val event = dto().toDomain()

        assertEquals("t-1", event.id)
        assertEquals("Show de Teste", event.name)
        assertEquals("Teatro de Teste", event.venue)
        assertEquals(Money(8_000), event.unitPrice)
        assertEquals(10, event.totalTickets)
    }

    @Test
    fun `AC-01 - dado data com offset quando mapear entao converte para o mesmo instante em UTC`() {
        val event = dto(startsAt = "2026-11-14T20:00:00-03:00").toDomain()

        assertEquals(Instant.parse("2026-11-14T23:00:00Z"), event.startsAt)
    }

    @Test
    fun `AC-03 - dado data fora do formato ISO-8601 quando mapear entao falha com erro de data`() {
        assertThrows<DateTimeParseException> { dto(startsAt = "14/11/2026 20:00").toDomain() }
    }

    @Test
    fun `AC-03 - dado data sem offset quando mapear entao falha por ser ambigua`() {
        assertThrows<DateTimeParseException> { dto(startsAt = "2026-11-14T20:00:00").toDomain() }
    }

    @Test
    fun `AC-03 - dado preco negativo quando mapear entao falha`() {
        assertThrows<IllegalArgumentException> { dto(priceCents = -1).toDomain() }
    }

    @Test
    fun `AC-03 - dado estoque negativo quando mapear entao falha`() {
        assertThrows<IllegalArgumentException> { dto(totalTickets = -1).toDomain() }
    }

    @Test
    fun `AC-03 - dado id nome ou local em branco quando mapear entao falha`() {
        assertThrows<IllegalArgumentException> { dto(id = " ").toDomain() }
        assertThrows<IllegalArgumentException> { dto(name = "").toDomain() }
        assertThrows<IllegalArgumentException> { dto(venue = " ").toDomain() }
    }

    @Test
    fun `AC-03 - dado ids repetidos no catalogo quando mapear a lista entao falha`() {
        assertThrows<IllegalArgumentException> { listOf(dto(id = "a"), dto(id = "a")).toDomain() }
    }

    @Test
    fun `AC-01 - dado lista de dtos quando mapear a lista entao mantem a ordem do arquivo`() {
        val events = listOf(dto(id = "b"), dto(id = "a")).toDomain()

        assertEquals(listOf("b", "a"), events.map { it.id })
    }
}
