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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Back arrow of the receipt (ui-spec SCR-03 4.1, 4.2): from the history always, from a sale only on `Error`. */
class ReceiptOriginTest {
    @Test
    fun `dado comprovante aberto do historico quando qualquer estado entao mostra a seta de voltar`() {
        assertTrue(showsNavigateUp(ReceiptOrigin.History, ReceiptUiState.Loading))
        assertTrue(showsNavigateUp(ReceiptOrigin.History, ReceiptUiState.Error))
    }

    @Test
    fun `AC-27 - dado comprovante ilegivel vindo da venda quando exibir o erro entao mostra a seta de voltar`() {
        assertTrue(showsNavigateUp(ReceiptOrigin.Sale, ReceiptUiState.Error))
    }

    @Test
    fun `dado comprovante vindo da venda quando carregar entao nao mostra a seta de voltar`() {
        assertFalse(showsNavigateUp(ReceiptOrigin.Sale, ReceiptUiState.Loading))
    }
}
