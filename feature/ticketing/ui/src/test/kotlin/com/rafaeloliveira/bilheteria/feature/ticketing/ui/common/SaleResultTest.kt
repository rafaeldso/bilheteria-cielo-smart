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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.common

import androidx.compose.ui.semantics.LiveRegionMode
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusBadgeVariant
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** The result of a sale shared by the receipt (SCR-03) and the history (SCR-04). */
class SaleResultTest {
    @Test
    fun `AC-18 - dado status de venda quando mapear entao aprovada negada e cancelada tem resultado`() {
        assertEquals(SaleResult.Approved, PurchaseStatus.Approved.toSaleResult())
        assertEquals(SaleResult.Declined, PurchaseStatus.Declined.toSaleResult())
        assertEquals(SaleResult.Cancelled, PurchaseStatus.Cancelled.toSaleResult())
    }

    @Test
    fun `AC-18 - dado status que nao e venda quando mapear entao nao tem resultado`() {
        assertNull(PurchaseStatus.Pending.toSaleResult())
        assertNull(PurchaseStatus.Recovering.toSaleResult())
        assertNull(PurchaseStatus.Failed.toSaleResult())
    }

    @Test
    fun `dado cada resultado quando escolher o badge entao cor e icone distinguem sucesso erro e neutro`() {
        assertEquals(DsStatusBadgeVariant.Success, SaleResult.Approved.badge)
        assertEquals(DsStatusBadgeVariant.Error, SaleResult.Declined.badge)
        assertEquals(DsStatusBadgeVariant.Neutral, SaleResult.Cancelled.badge)
    }

    @Test
    fun `dado aprovada quando anunciar entao e status educado e dado negada ou cancelada entao e alerta assertivo`() {
        assertEquals(LiveRegionMode.Polite, SaleResult.Approved.liveRegion)
        assertEquals(LiveRegionMode.Assertive, SaleResult.Declined.liveRegion)
        assertEquals(LiveRegionMode.Assertive, SaleResult.Cancelled.liveRegion)
    }
}
