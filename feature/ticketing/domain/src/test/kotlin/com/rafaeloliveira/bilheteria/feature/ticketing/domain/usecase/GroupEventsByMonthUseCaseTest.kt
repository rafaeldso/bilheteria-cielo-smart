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

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.TEST_NOW
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.anEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class GroupEventsByMonthUseCaseTest {
    private val useCase = GroupEventsByMonthUseCase(Clock.fixed(TEST_NOW, ZoneOffset.UTC))

    private val november = EventMonth(2026, 11)
    private val december = EventMonth(2026, 12)

    private fun available(
        id: String,
        startsAt: String,
    ) = AvailableEvent(anEvent(id = id, startsAt = Instant.parse(startsAt)), available = 10)

    private val nov20 = available("nov20", "2026-11-20T20:00:00Z")
    private val nov05 = available("nov05", "2026-11-05T20:00:00Z")
    private val dec10 = available("dec10", "2026-12-10T20:00:00Z")

    @Test
    fun `AC-22 - dado eventos em dois meses quando abrir entao agrupa por mes em ordem de data`() {
        val result = useCase(listOf(dec10, nov20, nov05), selected = null)

        assertEquals(listOf(november, december), result.months)
        assertEquals(listOf(november, december), result.groups.map { it.month })
        assertEquals(listOf(nov05, nov20), result.groups[0].events)
        assertEquals(listOf(dec10), result.groups[1].events)
    }

    @Test
    fun `AC-22 - dado eventos em anos diferentes quando agrupar entao meses seguem a ordem cronologica`() {
        val jan = available("jan", "2027-01-15T20:00:00Z")

        val result = useCase(listOf(jan, dec10), selected = null)

        assertEquals(listOf(december, EventMonth(2027, 1)), result.months)
    }

    @Test
    fun `AC-22 - dado evento na virada de mes quando agrupar entao usa o fuso do relogio`() {
        val lateNight = available("late", "2026-12-01T01:00:00Z")
        val saoPaulo = GroupEventsByMonthUseCase(Clock.fixed(TEST_NOW, ZoneId.of("America/Sao_Paulo")))

        assertEquals(listOf(november), saoPaulo(listOf(lateNight), selected = null).months)
        assertEquals(listOf(december), useCase(listOf(lateNight), selected = null).months)
    }

    @Test
    fun `AC-22 - dado nenhum evento quando agrupar entao nao ha meses nem grupos`() {
        val result = useCase(emptyList(), selected = null)

        assertEquals(emptyList<EventMonth>(), result.months)
        assertNull(result.selected)
        assertEquals(emptyList<MonthGroup>(), result.groups)
    }

    @Test
    fun `AC-23 - dado filtro de mes quando selecionar dezembro entao so eventos de dezembro`() {
        val result = useCase(listOf(nov05, nov20, dec10), selected = december)

        assertEquals(december, result.selected)
        assertEquals(listOf(november, december), result.months)
        assertEquals(listOf(MonthGroup(december, listOf(dec10))), result.groups)
    }

    @Test
    fun `AC-23 - dado meses sem evento quando listar filtros entao so aparecem meses com evento`() {
        val result = useCase(listOf(nov05, dec10), selected = null)

        assertEquals(listOf(november, december), result.months)
        assertEquals(false, EventMonth(2027, 1) in result.months)
    }

    @Test
    fun `AC-23 - dado nenhum filtro quando selecionar todos entao mostra todos os meses`() {
        val result = useCase(listOf(nov05, dec10), selected = null)

        assertNull(result.selected)
        assertEquals(listOf(november, december), result.groups.map { it.month })
    }

    @Test
    fun `AC-24 - dado mes esgotado quando voltar a lista entao filtro volta para todos`() {
        val result = useCase(listOf(nov05, nov20), selected = december)

        assertNull(result.selected)
        assertEquals(listOf(november), result.months)
        assertEquals(listOf(MonthGroup(november, listOf(nov05, nov20))), result.groups)
    }

    @Test
    fun `AC-24 - dado lista vazia quando houver filtro selecionado entao filtro volta para todos`() {
        val result = useCase(emptyList(), selected = november)

        assertNull(result.selected)
        assertEquals(emptyList<MonthGroup>(), result.groups)
    }
}
