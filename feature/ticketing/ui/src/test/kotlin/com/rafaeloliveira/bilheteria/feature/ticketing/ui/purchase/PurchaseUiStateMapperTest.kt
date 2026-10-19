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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.FailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.testing.aPurchase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PurchaseUiStateMapperTest {
    @Test
    fun `dado tentativa falha e catalogo ainda desconhecido quando mapear entao exibe carregando e nao libera pagar`() {
        val failed = aPurchase(status = PurchaseStatus.Failed, failureReason = FailureReason.Generic)
        val inputs =
            PurchaseInputs(
                open = OpenPurchase.None,
                outcome = AttemptOutcome.Finished(failed),
                event = EventLookup.Unknown,
                quantity = 1,
                recovery = Recovery.Idle,
            )

        assertEquals(PurchaseUiState.Loading, inputs.toUiState())
    }
}
