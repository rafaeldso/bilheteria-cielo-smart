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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation

import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptOrigin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The pure decisions of the navigation graph (ui-spec flow, plan section 4). */
class TicketingNavigationRulesTest {
    @Test
    fun `dado historico logo abaixo do comprovante quando abrir entao a origem e o historico`() {
        assertEquals(ReceiptOrigin.History, receiptOrigin(previousIsHistory = true))
    }

    @Test
    fun `dado lista de eventos logo abaixo do comprovante quando abrir entao a origem e a venda`() {
        assertEquals(ReceiptOrigin.Sale, receiptOrigin(previousIsHistory = false))
    }

    @Test
    fun `AC-15 - dado app aberto na lista de eventos com compra aberta quando decidir entao retoma a compra`() {
        assertTrue(shouldRedirect(currentIsEventList = true))
    }

    @Test
    fun `AC-15 - dado pilha restaurada fora da lista de eventos quando decidir entao nao empilha a compra de novo`() {
        assertFalse(shouldRedirect(currentIsEventList = false))
    }
}
