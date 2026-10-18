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

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.LiveRegionMode
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsStatusBadgeVariant
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PurchaseStatus
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R

/**
 * The three results of a sale (BR-18), shared by the receipt (SCR-03) and the history (SCR-04) so both show the same
 * badge and the same word for the same result (ui-spec SCR-03/SCR-04, 4.3).
 *
 * @property badge color and icon of the status badge: green, red and neutral.
 * @property liveRegion how the receipt announces the result (ui-spec 4.6): an approval is a status (polite); a
 * decline or a cancellation needs the operator's attention (assertive).
 * @property statusRes the word on the badge and in the "Status" field ("Aprovada", "Negada", "Cancelada").
 */
internal enum class SaleResult(
    val badge: DsStatusBadgeVariant,
    val liveRegion: LiveRegionMode,
    @StringRes val statusRes: Int,
) {
    Approved(DsStatusBadgeVariant.Success, LiveRegionMode.Polite, R.string.ticketing_status_approved),
    Declined(DsStatusBadgeVariant.Error, LiveRegionMode.Assertive, R.string.ticketing_status_declined),
    Cancelled(DsStatusBadgeVariant.Neutral, LiveRegionMode.Assertive, R.string.ticketing_status_cancelled),
}

/**
 * The result of a sale, or null for a status that is not a sale. The use case already keeps only sales (BR-18), so
 * null is a defensive branch: the row is not drawn instead of being shown with a wrong label.
 */
internal fun PurchaseStatus.toSaleResult(): SaleResult? =
    when (this) {
        PurchaseStatus.Approved -> SaleResult.Approved
        PurchaseStatus.Declined -> SaleResult.Declined
        PurchaseStatus.Cancelled -> SaleResult.Cancelled
        PurchaseStatus.Pending, PurchaseStatus.Recovering, PurchaseStatus.Failed -> null
    }
